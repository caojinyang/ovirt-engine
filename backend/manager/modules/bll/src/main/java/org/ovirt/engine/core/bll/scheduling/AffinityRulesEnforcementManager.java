package org.ovirt.engine.core.bll.scheduling;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.ovirt.engine.core.bll.interfaces.BackendInternal;
import org.ovirt.engine.core.bll.job.ExecutionHandler;
import org.ovirt.engine.core.bll.scheduling.arem.AffinityRulesEnforcer;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.BackendService;
import org.ovirt.engine.core.common.action.ActionType;
import org.ovirt.engine.core.common.action.MigrateVmParameters;
import org.ovirt.engine.core.common.businessentities.Cluster;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.config.Config;
import org.ovirt.engine.core.common.config.ConfigValues;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogDirector;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogableImpl;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.MessageBundler;
import org.ovirt.engine.core.dao.ClusterDao;
import org.ovirt.engine.core.utils.threadpool.ThreadPools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class AffinityRulesEnforcementManager implements BackendService {

    protected Logger log = LoggerFactory.getLogger(getClass());

    @Inject
    private AuditLogDirector auditLogDirector;
    @Inject
    private ClusterDao clusterDao;
    @Inject
    private AffinityRulesEnforcer rulesEnforcer;
    @Inject
    private BackendInternal backend;
    @Inject
    @ThreadPools(ThreadPools.ThreadPoolType.EngineScheduledThreadPool)
    private ManagedScheduledExecutorService executor;

    @PostConstruct
    protected void wakeup() {

        auditLogDirector.log(new AuditLogableImpl(), AuditLogType.AFFINITY_RULES_ENFORCEMENT_MANAGER_START);

        scheduleJobs(getRegularInterval(), getInitialInterval());
    }

    private Integer getInitialInterval() {
        return Config.<Integer>getValue(ConfigValues.AffinityRulesEnforcementManagerInitialDelay);
    }

    /**
     * Regular interval is used when the manager is migrating VMs and enforcing affinity rules.
     *
     * @return - The regular interval from the ConfigValues.
     */
    private Integer getRegularInterval() {
        return Config.<Integer>getValue(ConfigValues.AffinityRulesEnforcementManagerRegularInterval);
    }

    /**
     * refresh method is called each interval of AffinityRulesEnforcementManager. It will try to find a broken affinity rule, choose a VM then, migrate it in order
     * to fix the breakage.
     */
    public void refresh() {
        try {
            log.debug("Affinity Rules Enforcement Manager interval reached.");

            final List<VM> vmCandidates = new ArrayList<>();
            for (Cluster cluster : clusterDao.getWithoutMigratingVms()) {
                if (!cluster.isInUpgradeMode()) {
                    final VM candidate = rulesEnforcer.chooseNextVmToMigrate(cluster);
                    if (candidate != null) {
                        vmCandidates.add(candidate);
                    }
                }
            }

            // Trigger migrations
            for (VM vm : vmCandidates) {
                migrateVM(vm);
            }
        } catch (Throwable t) {
            log.error("Exception in refreshing affinity rules: {}", ExceptionUtils.getRootCauseMessage(t));
            log.debug("Exception", t);
        }
    }

    protected void migrateVM(final VM vmToMigrate) {
        MigrateVmParameters parameters = new MigrateVmParameters(false, vmToMigrate.getId());

        parameters.setReason(MessageBundler.getMessage(AuditLogType.MIGRATION_REASON_AFFINITY_ENFORCEMENT));

        backend.runInternalAction(ActionType.BalanceVm,
                parameters,
                ExecutionHandler.createInternalJobContext());
    }

    private void scheduleJobs(long regularInterval, long initialInterval) {
        executor.scheduleWithFixedDelay(this::refresh,
                initialInterval,
                regularInterval,
                TimeUnit.MINUTES);
    }
}

package org.ovirt.engine.api.restapi.resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.core.Response;

import org.ovirt.engine.api.model.ConfigurationType;
import org.ovirt.engine.api.model.Disk;
import org.ovirt.engine.api.model.DiskAttachment;
import org.ovirt.engine.api.model.DiskAttachments;
import org.ovirt.engine.api.model.Disks;
import org.ovirt.engine.api.model.Snapshot;
import org.ovirt.engine.api.model.Snapshots;
import org.ovirt.engine.api.model.Vm;
import org.ovirt.engine.api.resource.SnapshotResource;
import org.ovirt.engine.api.resource.SnapshotsResource;
import org.ovirt.engine.api.restapi.types.DiskMapper;
import org.ovirt.engine.api.restapi.types.SnapshotMapper;
import org.ovirt.engine.core.common.action.ActionType;
import org.ovirt.engine.core.common.action.CreateAllSnapshotsFromVmParameters;
import org.ovirt.engine.core.common.businessentities.storage.BaseDisk;
import org.ovirt.engine.core.common.businessentities.storage.DiskImage;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.common.queries.QueryReturnValue;
import org.ovirt.engine.core.common.queries.QueryType;
import org.ovirt.engine.core.compat.Guid;

public class BackendSnapshotsResource
        extends AbstractBackendCollectionResource<Snapshot, org.ovirt.engine.core.common.businessentities.Snapshot>
        implements SnapshotsResource {

    protected Guid parentId;

    public BackendSnapshotsResource(Guid parentId) {
        super(Snapshot.class, org.ovirt.engine.core.common.businessentities.Snapshot.class);
        this.parentId = parentId;
    }

    @Override
    public Snapshots list() {
        return mapCollection(getBackendCollection(QueryType.GetAllVmSnapshotsByVmId,
                new IdQueryParameters(parentId)));
    }

    @Override
    public Response add(Snapshot snapshot) {
        return doAdd(snapshot, expectBlocking());
    }

    protected Response doAdd(Snapshot snapshot, boolean block) {
        validateParameters(snapshot, "description");
        CreateAllSnapshotsFromVmParameters snapshotParams =
            new CreateAllSnapshotsFromVmParameters(parentId, snapshot.getDescription());
        if (snapshot.isSetPersistMemorystate()) {
            snapshotParams.setSaveMemory(snapshot.isPersistMemorystate());
        }
        if (snapshot.isSetDiskAttachments()) {
            snapshotParams.setDiskIds(mapDisks(snapshot.getDiskAttachments()));
        }
        return performCreate(ActionType.CreateAllSnapshotsFromVm,
                               snapshotParams,
                               new SnapshotIdResolver(),
                               block);
    }

    private Set<Guid> mapDisks(DiskAttachments diskAttachments) {
        Set<Guid> diskIds = null;
        if (diskAttachments.isSetDiskAttachments()) {
            diskIds =
                    diskAttachments.getDiskAttachments().stream()
                            .map(DiskAttachment::getDisk)
                            .filter(Objects::nonNull)
                            .map(d -> (DiskImage) DiskMapper.map(d, null))
                            .map(BaseDisk::getId)
                            .collect(Collectors.toSet());
        }
        return diskIds;
    }

    public ArrayList<DiskImage> mapDisks(Disks disks) {
        ArrayList<DiskImage> diskImages = null;
        if (disks != null && disks.isSetDisks()) {
            diskImages = new ArrayList<>();
            for (Disk disk : disks.getDisks()) {
                if (disk != null) {
                    DiskImage diskImage = (DiskImage) DiskMapper.map(disk, null);
                    diskImages.add(diskImage);
                }
            }
        }
        return diskImages;
    }

    @Override
    public SnapshotResource getSnapshotResource(String id) {
        return inject(new BackendSnapshotResource(id, parentId, this));
    }

    protected Snapshots mapCollection(List<org.ovirt.engine.core.common.businessentities.Snapshot> entities) {
        Snapshots snapshots = new Snapshots();
        for (org.ovirt.engine.core.common.businessentities.Snapshot entity : entities) {
            Snapshot snapshot = map(entity, null);
            snapshot = populate(snapshot, entity);
            snapshot = addLinks(snapshot);
            snapshot = addVmConfiguration(entity, snapshot);
            snapshots.getSnapshots().add(snapshot);
        }
        return snapshots;
    }

    protected Snapshot addVmConfiguration(org.ovirt.engine.core.common.businessentities.Snapshot entity, Snapshot snapshot) {
        if (entity.isVmConfigurationAvailable()) {
            snapshot.setVm(new Vm());
            getMapper(org.ovirt.engine.core.common.businessentities.VM.class, Vm.class).map(getVmPreview(snapshot), snapshot.getVm());
        }
        else {
            snapshot.setVm(null);
            snapshot.getLinks().clear();
        }
        return snapshot;
    }

    protected org.ovirt.engine.core.common.businessentities.VM getVmPreview(Snapshot snapshot) {
        org.ovirt.engine.core.common.businessentities.VM vm = getEntity(org.ovirt.engine.core.common.businessentities.VM.class, QueryType.GetVmConfigurationBySnapshot, new IdQueryParameters(asGuid(snapshot.getId())), null);
        return vm;
    }

    protected org.ovirt.engine.core.common.businessentities.Snapshot getSnapshotById(Guid id) {
        //TODO: move to 'GetSnapshotBySnapshotId' once Backend supplies it.
        for (org.ovirt.engine.core.common.businessentities.Snapshot snapshot : getBackendCollection(QueryType.GetAllVmSnapshotsByVmId,
                new IdQueryParameters(parentId))) {
            if (snapshot.getId().equals(id)) {
                return snapshot;
            }
        }
        return null;
    }

    @Override
    protected Snapshot addParents(Snapshot snapshot) {
        snapshot.setVm(new Vm());
        snapshot.getVm().setId(parentId.toString());
        return snapshot;
    }

    protected class SnapshotIdResolver extends EntityIdResolver<Guid> {

        SnapshotIdResolver() {}

        @Override
        public org.ovirt.engine.core.common.businessentities.Snapshot lookupEntity(
                Guid id) throws BackendFailureException {
            return getSnapshotById(id);
        }
    }

    @Override
    protected Snapshot doPopulate(Snapshot model, org.ovirt.engine.core.common.businessentities.Snapshot entity) {
        return populateSnapshotConfiguration(model);
    }

    private Snapshot populateSnapshotConfiguration (Snapshot model) {
        QueryReturnValue queryReturnValue =
                runQuery(QueryType.GetSnapshotBySnapshotId,
                        new IdQueryParameters(Guid.createGuidFromString(model.getId())));

        if (queryReturnValue.getSucceeded() && queryReturnValue.getReturnValue() != null) {
            org.ovirt.engine.core.common.businessentities.Snapshot snapshot = queryReturnValue.getReturnValue();
            if (snapshot.getVmConfiguration() != null) {
                return SnapshotMapper.map(snapshot.getVmConfiguration(),
                        ConfigurationType.OVF,
                        model);
            }
        }

        return model;
    }
}

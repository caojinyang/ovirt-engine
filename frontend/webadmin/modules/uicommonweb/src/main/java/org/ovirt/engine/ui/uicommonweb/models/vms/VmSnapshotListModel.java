package org.ovirt.engine.ui.uicommonweb.models.vms;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.ovirt.engine.core.common.ActionUtils;
import org.ovirt.engine.core.common.action.ActionReturnValue;
import org.ovirt.engine.core.common.action.ActionType;
import org.ovirt.engine.core.common.action.AddVmFromSnapshotParameters;
import org.ovirt.engine.core.common.action.AddVmTemplateFromSnapshotParameters;
import org.ovirt.engine.core.common.action.RemoveSnapshotParameters;
import org.ovirt.engine.core.common.action.RestoreAllSnapshotsParameters;
import org.ovirt.engine.core.common.action.TryBackToAllSnapshotsOfVmParameters;
import org.ovirt.engine.core.common.businessentities.Snapshot;
import org.ovirt.engine.core.common.businessentities.Snapshot.SnapshotStatus;
import org.ovirt.engine.core.common.businessentities.Snapshot.SnapshotType;
import org.ovirt.engine.core.common.businessentities.SnapshotActionEnum;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VMStatus;
import org.ovirt.engine.core.common.businessentities.storage.Disk;
import org.ovirt.engine.core.common.businessentities.storage.DiskImage;
import org.ovirt.engine.core.common.businessentities.storage.DiskStorageType;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.common.queries.QueryType;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.StringHelper;
import org.ovirt.engine.ui.frontend.Frontend;
import org.ovirt.engine.ui.uicommonweb.Linq;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.builders.BuilderExecutor;
import org.ovirt.engine.ui.uicommonweb.builders.template.UnitToAddVmTemplateParametersBuilder;
import org.ovirt.engine.ui.uicommonweb.builders.template.VmBaseToVmBaseForTemplateCompositeBaseBuilder;
import org.ovirt.engine.ui.uicommonweb.builders.vm.CommonUnitToVmBaseBuilder;
import org.ovirt.engine.ui.uicommonweb.builders.vm.FullUnitToVmBaseBuilder;
import org.ovirt.engine.ui.uicommonweb.builders.vm.UnitToGraphicsDeviceParamsBuilder;
import org.ovirt.engine.ui.uicommonweb.builders.vm.VmSpecificUnitToVmBuilder;
import org.ovirt.engine.ui.uicommonweb.dataprovider.AsyncDataProvider;
import org.ovirt.engine.ui.uicommonweb.help.HelpTag;
import org.ovirt.engine.ui.uicommonweb.models.ConfirmationModel;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.Model;
import org.ovirt.engine.ui.uicommonweb.models.SearchableListModel;
import org.ovirt.engine.ui.uicompat.ConstantsManager;
import org.ovirt.engine.ui.uicompat.PropertyChangedEventArgs;

import com.google.gwt.i18n.client.DateTimeFormat;

public class VmSnapshotListModel extends SearchableListModel<VM, Snapshot> {
    // This constant is intended to be exported to a generic UTILS class later on
    private static final String DATE_FORMAT = "yyyy-MM-dd, HH:mm"; //$NON-NLS-1$

    private UICommand newCommand;

    public UICommand getNewCommand() {
        return newCommand;
    }

    private void setNewCommand(UICommand value) {
        newCommand = value;
    }

    private UICommand previewCommand;

    public UICommand getPreviewCommand() {
        return previewCommand;
    }

    private void setPreviewCommand(UICommand value) {
        previewCommand = value;
    }

    private UICommand customPreviewCommand;

    public UICommand getCustomPreviewCommand() {
        return customPreviewCommand;
    }

    private void setCustomPreviewCommand(UICommand value) {
        customPreviewCommand = value;
    }

    private UICommand commitCommand;

    public UICommand getCommitCommand() {
        return commitCommand;
    }

    private void setCommitCommand(UICommand value) {
        commitCommand = value;
    }

    private UICommand undoCommand;

    public UICommand getUndoCommand() {
        return undoCommand;
    }

    private void setUndoCommand(UICommand value) {
        undoCommand = value;
    }

    private UICommand removeCommand;

    public UICommand getRemoveCommand() {
        return removeCommand;
    }

    private void setRemoveCommand(UICommand value) {
        removeCommand = value;
    }

    private UICommand cloneVmCommand;

    public UICommand getCloneVmCommand() {
        return cloneVmCommand;
    }

    private void setCloneVmCommand(UICommand value) {
        cloneVmCommand = value;
    }

    private UICommand cloneTemplateCommand;

    public UICommand getCloneTemplateCommand() {
        return cloneTemplateCommand;
    }

    public void setCloneTemplateCommand(UICommand cloneTemplateCommand) {
        this.cloneTemplateCommand = cloneTemplateCommand;
    }

    private EntityModel<Boolean> canSelectSnapshot;

    public EntityModel<Boolean> getCanSelectSnapshot() {
        return canSelectSnapshot;
    }

    private void setCanSelectSnapshot(EntityModel<Boolean> value) {
        canSelectSnapshot = value;
    }

    private HashMap<Guid, SnapshotModel> snapshotsMap;

    public HashMap<Guid, SnapshotModel> getSnapshotsMap() {
        return snapshotsMap;
    }

    public void setSnapshotsMap(HashMap<Guid, SnapshotModel> value) {
        snapshotsMap = value;
        onPropertyChanged(new PropertyChangedEventArgs("SnapshotsMap")); //$NON-NLS-1$
    }

    private boolean memorySnapshotSupported;

    public boolean isMemorySnapshotSupported() {
        return memorySnapshotSupported;
    }

    private void setMemorySnapshotSupported(boolean value) {
        if (memorySnapshotSupported != value) {
            memorySnapshotSupported = value;
            onPropertyChanged(new PropertyChangedEventArgs("IsMemorySnapshotSupported")); //$NON-NLS-1$
        }
    }

    private List<DiskImage> vmDisks;

    public List<DiskImage> getVmDisks() {
        return vmDisks;
    }

    public void setVmDisks(List<DiskImage> value) {
        vmDisks = value;
    }

    public VmSnapshotListModel() {
        setTitle(ConstantsManager.getInstance().getConstants().snapshotsTitle());
        setHelpTag(HelpTag.snapshots);
        setHashName("snapshots"); //$NON-NLS-1$

        setNewCommand(new UICommand("New", this)); //$NON-NLS-1$
        setPreviewCommand(new UICommand("Preview", this)); //$NON-NLS-1$
        setCustomPreviewCommand(new UICommand("CustomPreview", this)); //$NON-NLS-1$
        setCommitCommand(new UICommand("Commit", this)); //$NON-NLS-1$
        setUndoCommand(new UICommand("Undo", this)); //$NON-NLS-1$
        setRemoveCommand(new UICommand("Remove", this)); //$NON-NLS-1$
        setCloneVmCommand(new UICommand("CloneVM", this)); //$NON-NLS-1$
        setCloneTemplateCommand(new UICommand("CloneTemplate", this)); //$NON-NLS-1$

        setCanSelectSnapshot(new EntityModel<>());
        getCanSelectSnapshot().setEntity(true);

        setSnapshotsMap(new HashMap<>());
        setVmDisks(new ArrayList<>());
    }

    @Override
    public void setItems(Collection<Snapshot> value) {
        ArrayList<Snapshot> snapshots = value != null ? new ArrayList<>(value) : new ArrayList<>();

        snapshots.sort(Linq.SnapshotByCreationDateCommparer.reversed());
        ArrayList<Snapshot> sortedSnapshots = new ArrayList<>();
        boolean hasNoPreviewSnapshot = snapshots.stream().noneMatch(s -> s.getType() == SnapshotType.PREVIEW);

        for (Snapshot snapshot : snapshots) {
            SnapshotModel snapshotModel = snapshotsMap.computeIfAbsent(snapshot.getId(), id -> new SnapshotModel());
            snapshotModel.setEntity(snapshot);

            if ((snapshot.getType() == SnapshotType.ACTIVE && hasNoPreviewSnapshot)
                    || snapshot.getType() == SnapshotType.PREVIEW) {
                sortedSnapshots.add(0, snapshot);
            }
            else if (snapshot.getType() == SnapshotType.REGULAR || snapshot.getType() == SnapshotType.STATELESS) {
                sortedSnapshots.add(snapshot);
            }
        }

        if (sortedSnapshots.stream().anyMatch(s -> s.getStatus() == SnapshotStatus.IN_PREVIEW)) {
            updatePreviewedDiskSnapshots(sortedSnapshots);
        }
        else {
            updateItems(sortedSnapshots);
        }
    }

    private void updateItems(List<Snapshot> snapshots) {
        super.setItems(snapshots);

        // Try to select the last created snapshot (fallback to active snapshot)
        if (getSelectedItem() == null && !snapshots.isEmpty()) {
            setSelectedItem(snapshots.size() > 1 ? snapshots.get(1) : snapshots.get(0));
        }

        updateActionAvailability();
    }

    @Override
    public void setEntity(VM value) {
        updateIsMemorySnapshotSupported(value);
        super.setEntity(value);
        updateVmActiveDisks();
    }

    @Override
    protected void onEntityChanged() {
        super.onEntityChanged();

        if (getEntity() != null) {
            getSearchCommand().execute();
        }
    }

    @Override
    protected void syncSearch() {
        VM vm = getEntity();
        if (vm == null) {
            return;
        }

        super.syncSearch(QueryType.GetAllVmSnapshotsByVmId, new IdQueryParameters(vm.getId()));
    }

    @Override
    protected void onSelectedItemChanged() {
        super.onSelectedItemChanged();
        updateActionAvailability();
    }

    @Override
    protected void selectedItemsChanged() {
        super.selectedItemsChanged();
        updateActionAvailability();
    }

    private void remove() {
        if (getEntity() != null) {
            if (getWindow() != null) {
                return;
            }

            Snapshot snapshot = getSelectedItem();
            ConfirmationModel model = new ConfirmationModel();
            setWindow(model);
            model.setTitle(ConstantsManager.getInstance().getConstants().deleteSnapshotTitle());
            model.setHelpTag(HelpTag.delete_snapshot);
            model.setHashName("delete_snapshot"); //$NON-NLS-1$
            model.setMessage(ConstantsManager.getInstance()
                    .getMessages()
                    .areYouSureYouWantToDeleteSanpshot( DateTimeFormat.getFormat(DATE_FORMAT).format(snapshot.getCreationDate()),
                            snapshot.getDescription()));

            String unpluggedDisksNames = getUnpluggedDisksNames();
            if (unpluggedDisksNames != null) {
                model.setNote(ConstantsManager.getInstance().getMessages().liveMergeUnpluggedDisksNote(unpluggedDisksNames));
            }

            UICommand tempVar = UICommand.createDefaultOkUiCommand("OnRemove", this); //$NON-NLS-1$
            model.getCommands().add(tempVar);
            UICommand tempVar2 = UICommand.createCancelUiCommand("Cancel", this); //$NON-NLS-1$
            model.getCommands().add(tempVar2);
        }
    }

    private String getUnpluggedDisksNames() {
        ArrayList<Disk> unpluggedDisks = new ArrayList<>();
        for (Disk disk : getVmDisks()) {
            if (!disk.getPlugged()) {
                unpluggedDisks.add(disk);
            }
        }
        return VmModelHelper.getDiskLabelList(unpluggedDisks);
    }

    private void onRemove() {
        Snapshot snapshot = getSelectedItem();
        if (snapshot == null) {
            cancel();
            return;
        }

        VM vm = getEntity();
        if (vm != null) {
            Frontend.getInstance().runAction(ActionType.RemoveSnapshot,
                    new RemoveSnapshotParameters(snapshot.getId(), vm.getId()), null, null);
        }

        getCanSelectSnapshot().setEntity(false);

        cancel();
    }

    private void undo() {
        VM vm = getEntity();
        if (vm != null) {
            Frontend.getInstance().runAction(ActionType.RestoreAllSnapshots,
                    new RestoreAllSnapshotsParameters(vm.getId(), SnapshotActionEnum.UNDO),
                    null,
                    null);
        }
    }

    private void commit() {
        VM vm = getEntity();
        if (vm != null) {
            Frontend.getInstance().runAction(ActionType.RestoreAllSnapshots,
                    new RestoreAllSnapshotsParameters(vm.getId(), SnapshotActionEnum.COMMIT),
                    null,
                    null);
        }
    }

    private void preview() {
        VM vm = getEntity();
        if (vm == null) {
            return;
        }

        final Snapshot snapshot = getSelectedItem();
        AsyncDataProvider.getInstance().getVmConfigurationBySnapshot(new AsyncQuery<>(v -> {
            ArrayList<DiskImage> snapshotDisks = v.getDiskList();
            List<DiskImage> disksExcludedFromSnapshot = imagesSubtract(getVmDisks(), snapshotDisks);

            boolean showMemorySnapshotWarning = isMemorySnapshotSupported() && !snapshot.getMemoryVolume().isEmpty();
            boolean showPartialSnapshotWarning = !disksExcludedFromSnapshot.isEmpty();

            if (showMemorySnapshotWarning || showPartialSnapshotWarning) {
                SnapshotModel model = new SnapshotModel();
                model.setVmDisks(getVmDisks());
                model.setDisks(snapshotDisks);
                model.setShowMemorySnapshotWarning(showMemorySnapshotWarning);
                model.setShowPartialSnapshotWarning(showPartialSnapshotWarning);
                if (showMemorySnapshotWarning) {
                    model.setOldClusterVersionOfSnapshotWithMemory(v);
                }
                setWindow(model);

                model.setTitle(showPartialSnapshotWarning ?
                        ConstantsManager.getInstance().getConstants().previewPartialSnapshotTitle() :
                        ConstantsManager.getInstance().getConstants().previewSnapshotTitle());
                model.setHelpTag(showPartialSnapshotWarning ? HelpTag.preview_partial_snapshot : HelpTag.preview_snapshot);
                model.setHashName(showPartialSnapshotWarning ? "preview_partial_snapshot" : "preview_snapshot"); //$NON-NLS-1$ //$NON-NLS-2$

                addCommands(model, "OnPreview"); //$NON-NLS-1$
            } else {
                runTryBackToAllSnapshotsOfVm(null, v, snapshot, false, null);
            }
        }), snapshot.getId());
    }

    private void updateVmActiveDisks() {
        VM vm = getEntity();
        if (vm == null) {
            return;
        }

        AsyncDataProvider.getInstance().getVmDiskList(new AsyncQuery<>(disks -> {
            getVmDisks().clear();
            for (Disk disk : disks) {
                if (disk.getDiskStorageType() == DiskStorageType.LUN) {
                    continue;
                }

                DiskImage diskImage = (DiskImage) disk;
                getVmDisks().add(diskImage);
            }
        }), vm.getId());
    }

    private void updatePreviewedDiskSnapshots(final List<Snapshot> snapshots) {
        for (DiskImage diskImage : getVmDisks()) {
            if (diskImage.getSnapshots().size() <= 1) {
                continue;
            }

            Guid snapshotId = diskImage.getSnapshots().get(1).getVmSnapshotId();
            getSnapshotsMap().get(snapshotId).getEntity().getDiskImages().add(diskImage);
        }

        updateItems(snapshots);
    }

    private void customPreview() {
        VM vm = getEntity();
        if (vm == null) {
            return;
        }

        PreviewSnapshotModel model = new PreviewSnapshotModel();
        model.setVmId(vm.getId());
        model.initialize();

        // Update according to the selected snapshot
        Snapshot selectedSnapshot = getSelectedItem();
        if (selectedSnapshot != null) {
            model.setSnapshotModel(getSnapshotsMap().get(selectedSnapshot.getId()));
        }

        setWindow(model);

        model.setTitle(ConstantsManager.getInstance().getConstants().customPreviewSnapshotTitle());
        model.setHelpTag(HelpTag.custom_preview_snapshot);
        model.setHashName("custom_preview_snapshot"); //$NON-NLS-1$

        addCommands(model, "OnCustomPreview"); //$NON-NLS-1$
    }

    private void onPreview() {
        Snapshot snapshot = getSelectedItem();
        if (snapshot == null) {
            cancel();
            return;
        }

        VM vm = getEntity();
        SnapshotModel snapshotModel = (SnapshotModel) getWindow();
        boolean memory = false;
        List<DiskImage> disks = null;

        if (snapshotModel.isShowPartialSnapshotWarning()) {
            switch (snapshotModel.getPartialPreviewSnapshotOptions().getSelectedItem()) {
                case preserveActiveDisks:
                    // get snapshot disks
                    disks = snapshotModel.getDisks();
                    // add active disks missed from snapshot
                    disks.addAll(imagesSubtract(getVmDisks(), disks));
                    break;
                case excludeActiveDisks:
                    // nothing to do - default behaviour
                    break;
                case openCustomPreviewDialog:
                    setWindow(null);
                    getCustomPreviewCommand().execute();
                    return;
            }
        }

        if (snapshotModel.isShowMemorySnapshotWarning()) {
            memory = snapshotModel.getMemory().getEntity();
        }

        runTryBackToAllSnapshotsOfVm(snapshotModel, vm, snapshot, memory, disks);
    }

    private static List<DiskImage> imagesSubtract(Collection<DiskImage> images, Collection<DiskImage> imagesToSubtract) {
        Set<Guid> idsToSubtract = imagesToSubtract.stream().map(DiskImage::getId).collect(Collectors.toSet());
        return images.stream().filter(new Linq.IdsPredicate<>(idsToSubtract).negate()).collect(Collectors.toList());
    }

    private void onCustomPreview() {
        VM vm = getEntity();
        PreviewSnapshotModel previewSnapshotModel = (PreviewSnapshotModel) getWindow();
        Snapshot snapshot = previewSnapshotModel.getSnapshotModel().getEntity();
        boolean memory = Boolean.TRUE.equals(previewSnapshotModel.getSnapshotModel().getMemory().getEntity());
        List<DiskImage> disks = previewSnapshotModel.getSelectedDisks();

        runTryBackToAllSnapshotsOfVm(previewSnapshotModel, vm, snapshot, memory, disks);
    }

    private void runTryBackToAllSnapshotsOfVm(final Model model, VM vm, Snapshot snapshot, boolean memory, List<DiskImage> disks) {
        if (model != null) {
            model.startProgress();
        }

        Frontend.getInstance().runAction(ActionType.TryBackToAllSnapshotsOfVm, new TryBackToAllSnapshotsOfVmParameters(
            vm.getId(), snapshot.getId(), memory, disks),
                result -> {
                    if (model != null) {
                        model.stopProgress();
                    }

                    if (result.getReturnValue().getSucceeded()) {
                        cancel();
                    }
                });
    }

    private void newEntity() {
        VM vm = getEntity();
        if (vm == null || getWindow() != null) {
            return;
        }

        SnapshotModel model = SnapshotModel.createNewSnapshotModel(this);
        setWindow(model);
        model.setVm(vm);
        model.initialize();
    }

    private void addCommands(Model model, String okCommandName) {
        model.getCommands().add(UICommand.createDefaultOkUiCommand(okCommandName, this)); //$NON-NLS-1$
        model.getCommands().add(UICommand.createCancelUiCommand("Cancel", this)); //$NON-NLS-1$
    }

    private void cancel() {
        setWindow(null);
    }

    private void cloneTemplate() {
        Snapshot snapshot = getSelectedItem();
        if (snapshot == null) {
            return;
        }

        if (getWindow() != null) {
            return;
        }

        final UnitVmModel model = new UnitVmModel(createNewTemplateBehavior(), this);
        setWindow(model);
        model.startProgress();

        AsyncDataProvider.getInstance().getVmConfigurationBySnapshot(new AsyncQuery<>(vm -> {
            NewTemplateVmModelBehavior behavior = (NewTemplateVmModelBehavior) model.getBehavior();
            behavior.setVm(vm);

            model.setTitle(ConstantsManager.getInstance().getConstants().newTemplateTitle());
            model.setHelpTag(HelpTag.clone_template_from_snapshot);
            model.setHashName("clone_template_from_snapshot"); //$NON-NLS-1$
            model.setIsNew(true);
            model.setCustomPropertiesKeysList(AsyncDataProvider.getInstance().getCustomPropertiesList());
            model.initialize();
            model.getVmType().setSelectedItem(vm.getVmType());
            model.getIsHighlyAvailable().setEntity(vm.getStaticData().isAutoStartup());
            model.getCommands().add(
                    new UICommand("OnNewTemplate", VmSnapshotListModel.this) //$NON-NLS-1$
                            .setTitle(ConstantsManager.getInstance().getConstants().ok())
                            .setIsDefault(true));

            model.getCommands().add(UICommand.createCancelUiCommand("Cancel", VmSnapshotListModel.this)); //$NON-NLS-1$
            model.stopProgress();
        }), snapshot.getId());
    }

    protected NewTemplateVmModelBehavior createNewTemplateBehavior() {
        return new NewTemplateVmModelBehavior();
    }

    private void onCloneTemplate() {
        final UnitVmModel model = (UnitVmModel) getWindow();
        NewTemplateVmModelBehavior behavior = (NewTemplateVmModelBehavior) model.getBehavior();
        Snapshot snapshot = getSelectedItem();
        if (snapshot == null) {
            cancel();
            return;
        }

        final VM vm = behavior.getVm();

        if (!model.validate(false)) {
            model.setIsValid(false);
        }
        else  if (model.getIsSubTemplate().getEntity()) {
            postNameUniqueCheck(vm);
        }
        else {
            String name = model.getName().getEntity();

            // Check name unicitate.
            AsyncDataProvider.getInstance().isTemplateNameUnique(new AsyncQuery<>(
                            isNameUnique -> {
                                if (!isNameUnique) {
                                    model.getInvalidityReasons().clear();
                                    model.getName()
                                            .getInvalidityReasons()
                                            .add(ConstantsManager.getInstance()
                                                    .getConstants()
                                                    .nameMustBeUniqueInvalidReason());
                                    model.getName().setIsValid(false);
                                    model.setIsValid(false);
                                    model.fireValidationCompleteEvent();
                                }
                                else {
                                    postNameUniqueCheck(vm);
                                }

                            }),
                    name, model.getSelectedDataCenter().getId());
        }
    }

    private void postNameUniqueCheck(VM vm) {
        UnitVmModel model = (UnitVmModel) getWindow();

        VM newVm = buildVmOnNewTemplate(model, vm);

        AddVmTemplateFromSnapshotParameters parameters =
                new AddVmTemplateFromSnapshotParameters(newVm.getStaticData(),
                        model.getName().getEntity(),
                        model.getDescription().getEntity(),
                        getSelectedItem().getId());
        BuilderExecutor.build(model, parameters, new UnitToAddVmTemplateParametersBuilder());
        model.startProgress();
        Frontend.getInstance().runAction(ActionType.AddVmTemplateFromSnapshot,
                parameters,
                result -> {

                    VmSnapshotListModel vmSnapshotListModel = (VmSnapshotListModel) result.getState();
                    vmSnapshotListModel.getWindow().stopProgress();
                    ActionReturnValue returnValueBase = result.getReturnValue();
                    if (returnValueBase != null && returnValueBase.getSucceeded()) {
                        vmSnapshotListModel.cancel();
                    }

                }, this);
    }

    protected static VM buildVmOnNewTemplate(UnitVmModel model, VM vm) {
        VM resultVm = new VM();
        resultVm.setId(vm.getId());
        BuilderExecutor.build(model, resultVm.getStaticData(), new CommonUnitToVmBaseBuilder());
        BuilderExecutor.build(vm.getStaticData(), resultVm.getStaticData(), new VmBaseToVmBaseForTemplateCompositeBaseBuilder());
        return resultVm;
    }

    private void cloneVM() {
        Snapshot snapshot = getSelectedItem();
        if (snapshot == null) {
            return;
        }

        if (getWindow() != null) {
            return;
        }

        VM selectedVm = getEntity();

        UnitVmModel model = new UnitVmModel(new CloneVmFromSnapshotModelBehavior(), this);
        model.getVmType().setSelectedItem(selectedVm.getVmType());
        model.setIsAdvancedModeLocalStorageKey("wa_snapshot_dialog");  //$NON-NLS-1$
        setWindow(model);

        model.startProgress();

        AsyncDataProvider.getInstance().getVmConfigurationBySnapshot(new AsyncQuery<>(vm -> {
            UnitVmModel unitVmModel = (UnitVmModel) getWindow();

            CloneVmFromSnapshotModelBehavior behavior = (CloneVmFromSnapshotModelBehavior) unitVmModel.getBehavior();
            behavior.setVm(vm);

            unitVmModel.setTitle(ConstantsManager.getInstance().getConstants().cloneVmFromSnapshotTitle());
            unitVmModel.setHelpTag(HelpTag.clone_vm_from_snapshot);
            unitVmModel.setHashName("clone_vm_from_snapshot"); //$NON-NLS-1$
            unitVmModel.setCustomPropertiesKeysList(AsyncDataProvider.getInstance().getCustomPropertiesList());
            unitVmModel.initialize();

            VmBasedWidgetSwitchModeCommand switchModeCommand = new VmBasedWidgetSwitchModeCommand();
            switchModeCommand.init(unitVmModel);
            unitVmModel.getCommands().add(switchModeCommand);

            UICommand tempVar = UICommand.createDefaultOkUiCommand("OnCloneVM", VmSnapshotListModel.this); //$NON-NLS-1$
            unitVmModel.getCommands().add(tempVar);
            UICommand tempVar2 = UICommand.createCancelUiCommand("Cancel", VmSnapshotListModel.this); //$NON-NLS-1$
            unitVmModel.getCommands().add(tempVar2);

            stopProgress();
        }), snapshot.getId());
    }

    private void onCloneVM() {
        UnitVmModel model = (UnitVmModel) getWindow();
        CloneVmFromSnapshotModelBehavior behavior = (CloneVmFromSnapshotModelBehavior) model.getBehavior();
        Snapshot snapshot = getSelectedItem();
        if (snapshot == null) {
            cancel();
            return;
        }

        if (!model.validate()) {
            return;
        }

        VM vm = behavior.getVm();

        // Save changes.
        buildVmOnClone(model, vm);

        vm.setUseHostCpuFlags(model.getHostCpu().getEntity());
        vm.setDiskMap(behavior.getVm().getDiskMap());

        HashMap<Guid, DiskImage> imageToDestinationDomainMap =
                model.getDisksAllocationModel().getImageToDestinationDomainMap();

        AddVmFromSnapshotParameters parameters =
                new AddVmFromSnapshotParameters(vm.getStaticData(), snapshot.getId());
        parameters.setDiskInfoDestinationMap(imageToDestinationDomainMap);
        parameters.setConsoleEnabled(model.getIsConsoleDeviceEnabled().getEntity());
        parameters.setVirtioScsiEnabled(model.getIsVirtioScsiEnabled().getEntity());
        parameters.setBalloonEnabled(model.getMemoryBalloonDeviceEnabled().getEntity());

        BuilderExecutor.build(model, parameters, new UnitToGraphicsDeviceParamsBuilder());

        if (!StringHelper.isNullOrEmpty(model.getVmId().getEntity())) {
            parameters.setVmId(new Guid(model.getVmId().getEntity()));
        }

        model.startProgress();

        Frontend.getInstance().runAction(ActionType.AddVmFromSnapshot, parameters,
                result -> {

                    VmSnapshotListModel vmSnapshotListModel = (VmSnapshotListModel) result.getState();
                    vmSnapshotListModel.getWindow().stopProgress();
                    ActionReturnValue returnValueBase = result.getReturnValue();
                    if (returnValueBase != null && returnValueBase.getSucceeded()) {
                        vmSnapshotListModel.cancel();
                        vmSnapshotListModel.updateActionAvailability();
                    }
                }, this);
    }

    protected static void buildVmOnClone(UnitVmModel model, VM vm) {
        BuilderExecutor.build(model, vm.getStaticData(), new FullUnitToVmBaseBuilder());
        BuilderExecutor.build(model, vm, new VmSpecificUnitToVmBuilder());
    }

    public void updateActionAvailability() {
        if (getItems() == null) {
            // no need to update action availability
            return;
        }

        VM vm = getEntity();
        Snapshot snapshot = getSelectedItem();
        List<VM> vmList = vm != null ? Collections.singletonList(vm) : Collections.emptyList();

        boolean isVmDown = vm != null && vm.getStatus() == VMStatus.Down;
        boolean isVmImageLocked = vm != null && vm.getStatus() == VMStatus.ImageLocked;
        boolean isVmQualifiedForSnapshotMerge = vm != null && vm.getStatus().isQualifiedForSnapshotMerge();
        boolean isPreviewing = getItems().stream().anyMatch(s -> s.getStatus() == SnapshotStatus.IN_PREVIEW);
        boolean isLocked = getItems().stream().anyMatch(s -> s.getStatus() == SnapshotStatus.LOCKED);
        boolean isSelected = snapshot != null && snapshot.getType() != SnapshotType.ACTIVE;
        boolean isStateless = getItems().stream().anyMatch(s -> s.getType() == SnapshotType.STATELESS);
        boolean isVmConfigurationBroken = snapshot != null && snapshot.isVmConfigurationBroken();

        getCanSelectSnapshot().setEntity(!isPreviewing && !isLocked && !isStateless
                && ActionUtils.canExecute(vmList, VM.class, ActionType.CreateAllSnapshotsFromVm));
        getNewCommand().setIsExecutionAllowed(!isPreviewing && !isLocked && !isVmImageLocked && !isStateless);
        getPreviewCommand().setIsExecutionAllowed(isSelected && !isLocked && !isPreviewing && isVmDown && !isStateless);
        getCustomPreviewCommand().setIsExecutionAllowed(getPreviewCommand().getIsExecutionAllowed());
        getCommitCommand().setIsExecutionAllowed(isPreviewing && isVmDown && !isStateless);
        getUndoCommand().setIsExecutionAllowed(isPreviewing && isVmDown && !isStateless);
        getRemoveCommand().setIsExecutionAllowed(isSelected && !isLocked && !isPreviewing && !isStateless
                && isVmQualifiedForSnapshotMerge);
        getCloneVmCommand().setIsExecutionAllowed(isSelected && !isLocked && !isPreviewing
                && !isVmImageLocked && !isStateless && !isVmConfigurationBroken);
        getCloneTemplateCommand().setIsExecutionAllowed(isSelected && !isLocked && !isPreviewing
                && !isVmImageLocked && !isStateless && !isVmConfigurationBroken);
    }

    private void updateIsMemorySnapshotSupported(Object entity) {
        if (entity == null) {
            return;
        }

        VM vm = (VM) entity;

        setMemorySnapshotSupported(AsyncDataProvider.getInstance().isMemorySnapshotSupported(vm));
    }

    @Override
    public void executeCommand(UICommand command) {
        super.executeCommand(command);

        if (command == getNewCommand()) {
            newEntity();
        }
        else if (command == getPreviewCommand()) {
            preview();
        }
        else if (command == getCustomPreviewCommand()) {
            customPreview();
        }
        else if (command == getCommitCommand()) {
            commit();
        }
        else if (command == getUndoCommand()) {
            undo();
        }
        else if (command == getRemoveCommand()) {
            remove();
        }
        else if (command == getCloneVmCommand()) {
            cloneVM();
        }
        else if (command == getCloneTemplateCommand()) {
            cloneTemplate();
        }
        else if ("OnNewTemplate".equals(command.getName())) { //$NON-NLS-1$
            onCloneTemplate();
        }
        else if ("OnRemove".equals(command.getName())) { //$NON-NLS-1$
            onRemove();
        }
        else if ("Cancel".equals(command.getName())) { //$NON-NLS-1$
            cancel();
        }
        else if ("OnCloneVM".equals(command.getName())) { //$NON-NLS-1$
            onCloneVM();
        }
        else if ("OnPreview".equals(command.getName())) { //$NON-NLS-1$
            onPreview();
        }
        else if ("OnCustomPreview".equals(command.getName())) { //$NON-NLS-1$
            onCustomPreview();
        }
    }

    @Override
    protected String getListName() {
        return "VmSnapshotListModel"; //$NON-NLS-1$
    }

    @Override
    protected boolean isSingleSelectionOnly() {
        // Single selection model
        return true;
    }
}

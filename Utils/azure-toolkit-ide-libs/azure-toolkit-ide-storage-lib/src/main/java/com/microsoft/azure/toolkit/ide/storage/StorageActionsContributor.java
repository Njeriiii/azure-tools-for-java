/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.storage;

import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.ide.storage.action.OpenAzureStorageExplorerAction;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionGroup;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.action.IActionGroup;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.*;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import com.microsoft.azure.toolkit.lib.storage.AzureStorageAccount;
import com.microsoft.azure.toolkit.lib.storage.AzuriteStorageAccount;
import com.microsoft.azure.toolkit.lib.storage.StorageAccount;
import com.microsoft.azure.toolkit.lib.storage.blob.IBlobFile;
import com.microsoft.azure.toolkit.lib.storage.model.StorageFile;
import com.microsoft.azure.toolkit.lib.storage.share.IShareFile;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.Optional;

public class StorageActionsContributor implements IActionsContributor {
    public static final int INITIALIZE_ORDER = ResourceCommonActionsContributor.INITIALIZE_ORDER + 1;

    public static final String SERVICE_ACTIONS = "actions.storage.service";
    public static final String ACCOUNT_ACTIONS = "actions.storage.account";
    public static final String AZURITE_ACTIONS = "actions.storage.azurite";
    public static final String FILE_ACTIONS = "actions.storage.file";
    public static final String DIRECTORY_ACTIONS = "actions.storage.directory";
    public static final String CONTAINER_ACTIONS = "actions.storage.container";
    public static final String SHARE_ACTIONS = "actions.storage.share";
    public static final String QUEUE_ACTIONS = "actions.storage.queue";
    public static final String TABLE_ACTIONS = "actions.storage.table";
    public static final String STORAGE_MODULE_ACTIONS = "actions.storage.module";

    public static final Action.Id<AzResource> OPEN_AZURE_STORAGE_EXPLORER = Action.Id.of("user/storage.open_azure_storage_explorer.account");
    public static final Action.Id<StorageAccount> COPY_CONNECTION_STRING = Action.Id.of("user/storage.copy_connection_string.account");
    public static final Action.Id<StorageAccount> COPY_PRIMARY_KEY = Action.Id.of("user/storage.copy_primary_key.account");
    public static final Action.Id<ResourceGroup> GROUP_CREATE_ACCOUNT = Action.Id.of("user/storage.create_account.group");
    public static final Action.Id<AzuriteStorageAccount> START_AZURITE = Action.Id.of("user/storage.start_azurite");
    public static final Action.Id<AzuriteStorageAccount> STOP_AZURITE = Action.Id.of("user/storage.stop_azurite");
    public static final Action.Id<AzuriteStorageAccount> COPY_CONNECTION_STRING_AZURITE = Action.Id.of("user/storage.copy_connection_string_azurite");
    public static final Action.Id<AzuriteStorageAccount> COPY_PRIMARY_KEY_AZURITE = Action.Id.of("user/storage.copy_primary_key_azurite");
    public static final Action.Id<IBlobFile> CREATE_BLOB = Action.Id.of("user/storage.create_blob.blob");
    public static final Action.Id<StorageFile> OPEN_FILE = Action.Id.of("user/storage.open_file.file");
    public static final Action.Id<StorageFile> CREATE_FILE = Action.Id.of("user/storage.create_file.file");
    public static final Action.Id<StorageFile> CREATE_DIRECTORY = Action.Id.of("user/storage.create_directory.dir");
    public static final Action.Id<StorageFile> DOWNLOAD_FILE = Action.Id.of("user/storage.download_file.file");
    public static final Action.Id<StorageFile> UPLOAD_FILES = Action.Id.of("user/storage.upload_files.dir");
    public static final Action.Id<StorageFile> UPLOAD_FILE = Action.Id.of("user/storage.upload_file.file");
    public static final Action.Id<StorageFile> UPLOAD_FOLDER = Action.Id.of("user/storage.upload_folder.dir");
    public static final Action.Id<StorageFile> COPY_FILE_URL = Action.Id.of("user/storage.copy_file_url.file");
    public static final Action.Id<StorageFile> COPY_FILE_SAS_URL = Action.Id.of("user/storage.copy_file_sas_url.file");
    public static final Action.Id<StorageFile> DELETE_DIRECTORY = Action.Id.of("user/storage.delete_directory.dir");
    public static final Action.Id<Refreshable> REFRESH = Action.Id.of("user/storage.refresh.resource");
    public static final Action.Id<AzResource> DELETE = Action.Id.of("user/storage.delete_resource.resource");


    @Override
    public void registerActions(AzureActionManager am) {
        new Action<>(OPEN_AZURE_STORAGE_EXPLORER)
            .withLabel("Open Azure Storage Explorer")
            .withIdParam(AzResource::getName)
            .visibleWhen(s -> s instanceof AzResource)
            .enableWhen(s -> !(s instanceof StorageAccount) || s.getFormalStatus(true).isConnected())
            .withHandler(resource -> {
                if (resource instanceof StorageAccount) {
                    new OpenAzureStorageExplorerAction().openResource((StorageAccount) resource);
                } else if (resource instanceof AbstractAzResource && ((AbstractAzResource<?, ?, ?>) resource).getParent() instanceof StorageAccount) {
                    //noinspection unchecked
                    new OpenAzureStorageExplorerAction().openResource((AbstractAzResource<?, StorageAccount, ?>) resource);
                } else {
                    AzureMessager.getMessager().warning("Only Azure Storages can be opened with Azure Storage Explorer.");
                }
            })
            .withShortcut(am.getIDEDefaultShortcuts().edit())
            .register(am);

        new Action<>(COPY_CONNECTION_STRING)
            .withLabel("Copy Connection String")
            .withIdParam(AzResource::getName)
            .visibleWhen(s -> s instanceof StorageAccount)
            .enableWhen(s -> s.getFormalStatus(true).isConnected())
            .withHandler(r -> {
                copyContentToClipboard(r.getConnectionString());
                AzureMessager.getMessager().info("Connection string copied");
            })
            .register(am);

        new Action<>(COPY_CONNECTION_STRING_AZURITE)
                .withLabel("Copy Connection String")
                .withIdParam(AzResource::getName)
                .visibleWhen(s -> s instanceof AzuriteStorageAccount)
                .enableWhen(s -> s.getFormalStatus(true).isConnected())
                .withHandler(r -> {
                    copyContentToClipboard(r.getConnectionString());
                    AzureMessager.getMessager().info("Connection string copied");
                })
                .setAuthRequired(false)
                .register(am);

        new Action<>(START_AZURITE)
                .withLabel("Start Azurite")
                .visibleWhen(s -> s instanceof AzuriteStorageAccount)
                .enableWhen(s -> !s.getFormalStatus(true).isRunning())
                .setAuthRequired(false)
                .register(am);

        new Action<>(STOP_AZURITE)
                .withLabel("Stop Azurite")
                .visibleWhen(s -> s instanceof AzuriteStorageAccount)
                .enableWhen(s -> s.getFormalStatus(true).isRunning())
                .setAuthRequired(false)
                .register(am);

        new Action<>(COPY_PRIMARY_KEY)
            .withLabel("Copy Primary Key")
            .withIdParam(AzResource::getName)
            .visibleWhen(s -> s instanceof StorageAccount)
            .enableWhen(s -> s.getFormalStatus(true).isConnected())
            .withHandler(resource -> {
                copyContentToClipboard(resource.getKey());
                AzureMessager.getMessager().info("Primary key copied");
            })
            .register(am);

        new Action<>(COPY_PRIMARY_KEY_AZURITE)
                .withLabel("Copy Primary Key")
                .visibleWhen(s -> s instanceof AzuriteStorageAccount)
                .enableWhen(s -> s.getFormalStatus(true).isConnected())
                .withHandler(resource -> {
                    copyContentToClipboard(resource.getKey());
                    AzureMessager.getMessager().info("Primary key copied");
                })
                .setAuthRequired(false)
                .register(am);

        new Action<>(OPEN_FILE)
            .withLabel("Open in Editor")
            .withIdParam(AzResource::getName)
            .visibleWhen(s -> s instanceof StorageFile)
            .setAuthRequired(false)
            .register(am);

        new Action<>(CREATE_BLOB)
            .withLabel("Create Empty Blob")
            .withIcon(AzureIcons.Action.CREATE.getIconPath())
            .withIdParam(AzResource::getName)
            .visibleWhen(s -> s instanceof IBlobFile && ((StorageFile) s).isDirectory())
            .setAuthRequired(false)
            .register(am);

        new Action<>(CREATE_FILE)
            .withLabel("Create Empty File")
            .withIcon(AzureIcons.Action.CREATE.getIconPath())
            .withIdParam(AzResource::getName)
            .visibleWhen(s -> s instanceof IShareFile && ((StorageFile) s).isDirectory())
            .setAuthRequired(false)
            .register(am);

        new Action<>(CREATE_DIRECTORY)
            .withLabel("Create Subdirectory")
            .withIcon(AzureIcons.Action.CREATE.getIconPath())
            .withIdParam(AzResource::getName)
            .visibleWhen(s -> s instanceof IShareFile && ((StorageFile) s).isDirectory())
            .setAuthRequired(false)
            .register(am);

        new Action<>(CREATE_DIRECTORY)
            .withLabel("Upload Files")
            .withIcon(AzureIcons.Action.UPLOAD.getIconPath())
            .withIdParam(AzResource::getName)
            .visibleWhen(s -> s instanceof IShareFile && ((StorageFile) s).isDirectory())
            .setAuthRequired(false)
            .register(am);

        new Action<>(UPLOAD_FILES)
            .withLabel("Upload Files")
            .withIcon(AzureIcons.Action.UPLOAD.getIconPath())
            .withIdParam(AzResource::getName)
            .visibleWhen(s -> s instanceof StorageFile && ((StorageFile) s).isDirectory())
            .setAuthRequired(false)
            .register(am);

        new Action<>(UPLOAD_FILE)
            .withLabel("Upload File")
            .withIcon(AzureIcons.Action.UPLOAD.getIconPath())
            .withIdParam(AzResource::getName)
            .visibleWhen(s -> s instanceof StorageFile && !((StorageFile) s).isDirectory())
            .setAuthRequired(false)
            .register(am);

        new Action<>(UPLOAD_FOLDER)
            .withLabel("Upload Folder")
            .withIcon(AzureIcons.Action.UPLOAD.getIconPath())
            .withIdParam(AzResource::getName)
            .visibleWhen(s -> s instanceof StorageFile && ((StorageFile) s).isDirectory())
            .setAuthRequired(false)
            .register(am);

        new Action<>(DOWNLOAD_FILE)
            .withLabel("Download")
            .withIcon(AzureIcons.Action.DOWNLOAD.getIconPath())
            .withIdParam(AzResource::getName)
            .visibleWhen(s -> s instanceof StorageFile && !((StorageFile) s).isDirectory())
            .setAuthRequired(false)
            .register(am);

        new Action<>(COPY_FILE_URL)
            .withLabel("Copy URL")
            .withIdParam(AzResource::getName)
            .visibleWhen(s -> s instanceof StorageFile)
            .setAuthRequired(false)
            .register(am);

        new Action<>(COPY_FILE_SAS_URL)
            .withLabel("Generate and Copy SAS Token and URL")
            .withIdParam(AzResource::getName)
            .visibleWhen(s -> s instanceof StorageFile)
            .setAuthRequired(false)
            .register(am);

        new Action<>(DELETE_DIRECTORY)
            .withLabel("Delete Directory")
            .withIcon(AzureIcons.Action.DELETE.getIconPath())
            .withIdParam(AzResource::getName)
            .visibleWhen(s -> s instanceof StorageFile && ((StorageFile) s).isDirectory())
            .withHandler(s -> {
                if (AzureMessager.getMessager().confirm(AzureString.format("Are you sure to delete directory \"%s\" and all its contents?", s.getName()))) {
                    ((Deletable) s).delete();
                }
            })
            .withShortcut(am.getIDEDefaultShortcuts().delete())
            .setAuthRequired(false)
            .register(am);

        new Action<>(GROUP_CREATE_ACCOUNT)
            .withLabel("Storage Account")
            .withIdParam(AzResource::getName)
            .visibleWhen(s -> s instanceof ResourceGroup)
            .enableWhen(s -> s.getFormalStatus(true).isConnected())
            .register(am);

        final AzureActionManager.Shortcuts shortcuts = am.getIDEDefaultShortcuts();
        new Action<>(REFRESH)
                .withLabel("Refresh")
                .withIdParam(s -> Optional.ofNullable(s).map(r -> {
                    if (r instanceof AzResource) {
                        return ((AzResource) r).getName();
                    } else if (r instanceof AbstractAzResourceModule) {
                        return ((AbstractAzResourceModule<?, ?, ?>) r).getResourceTypeName();
                    }
                    throw new IllegalArgumentException("Unsupported type: " + r.getClass());
                }).orElse(null))
                .withIcon(AzureIcons.Action.REFRESH.getIconPath())
                .withShortcut(shortcuts.refresh())
                .visibleWhen(s -> s instanceof Refreshable)
                .withHandler(Refreshable::refresh)
                .setAuthRequired(false) // set auth required to false for local emulator
                .register(am);

        new Action<>(DELETE)
                .withLabel("Delete")
                .withIcon(AzureIcons.Action.DELETE.getIconPath())
                .withIdParam(AzResource::getName)
                .withShortcut(shortcuts.delete())
                .visibleWhen(s -> (s instanceof AzResource && s instanceof Deletable))
                .enableWhen(s -> {
                    if (s instanceof AbstractAzResource) {
                        final AbstractAzResource<?, ?, ?> r = (AbstractAzResource<?, ?, ?>) s;
                        return !r.getFormalStatus(true).isDeleted() && !r.isDraftForCreating();
                    }
                    return true;
                })
                .setAuthRequired(false)
                .withHandler((s) -> {
                    if (AzureMessager.getMessager().confirm(String.format("Are you sure to delete %s \"%s\"", s.getResourceTypeName(), s.getName()))) {
                        ((Deletable) s).delete();
                    }
                }).register(am);
    }

    @Override
    public void registerGroups(AzureActionManager am) {
        final ActionGroup serviceActionGroup = new ActionGroup(
            StorageActionsContributor.REFRESH,
            ResourceCommonActionsContributor.OPEN_AZURE_REFERENCE_BOOK,
            "---",
            ResourceCommonActionsContributor.CREATE
        );
        am.registerGroup(SERVICE_ACTIONS, serviceActionGroup);

        final ActionGroup accountActionGroup = new ActionGroup(
            ResourceCommonActionsContributor.PIN,
            "---",
            StorageActionsContributor.OPEN_AZURE_STORAGE_EXPLORER,
            "---",
            StorageActionsContributor.REFRESH,
            ResourceCommonActionsContributor.OPEN_AZURE_REFERENCE_BOOK,
            ResourceCommonActionsContributor.OPEN_PORTAL_URL,
            "---",
            StorageActionsContributor.COPY_CONNECTION_STRING,
            StorageActionsContributor.COPY_PRIMARY_KEY,
            "---",
            ResourceCommonActionsContributor.CONNECT,
            "---",
            StorageActionsContributor.DELETE
        );
        am.registerGroup(ACCOUNT_ACTIONS, accountActionGroup);

        final ActionGroup azuriteActionGroup = new ActionGroup(
                ResourceCommonActionsContributor.PIN,
                "---",
                StorageActionsContributor.OPEN_AZURE_STORAGE_EXPLORER,
                "---",
                StorageActionsContributor.REFRESH,
                ResourceCommonActionsContributor.OPEN_AZURE_REFERENCE_BOOK,
                ResourceCommonActionsContributor.OPEN_PORTAL_URL,
                "---",
                StorageActionsContributor.START_AZURITE,
                StorageActionsContributor.STOP_AZURITE,
                StorageActionsContributor.COPY_CONNECTION_STRING_AZURITE,
                StorageActionsContributor.COPY_PRIMARY_KEY_AZURITE,
                "---",
                ResourceCommonActionsContributor.CONNECT
        );
        am.registerGroup(AZURITE_ACTIONS, azuriteActionGroup);

        final ActionGroup moduleActionGroup = new ActionGroup(
            StorageActionsContributor.REFRESH,
            "---",
            ResourceCommonActionsContributor.CREATE
        );
        am.registerGroup(STORAGE_MODULE_ACTIONS, moduleActionGroup);

        final ActionGroup fileActionGroup = new ActionGroup(
            StorageActionsContributor.REFRESH,
            "---",
            StorageActionsContributor.CREATE_BLOB,
            StorageActionsContributor.CREATE_FILE,
            StorageActionsContributor.CREATE_DIRECTORY,
            "---",
            StorageActionsContributor.DOWNLOAD_FILE,
            "---",
            StorageActionsContributor.COPY_FILE_URL,
            StorageActionsContributor.COPY_FILE_SAS_URL,
            "---",
            StorageActionsContributor.DELETE
        );
        am.registerGroup(FILE_ACTIONS, fileActionGroup);

        final ActionGroup dirActionGroup = new ActionGroup(
            StorageActionsContributor.REFRESH,
            "---",
            StorageActionsContributor.CREATE_BLOB,
            StorageActionsContributor.CREATE_FILE,
            StorageActionsContributor.CREATE_DIRECTORY,
            "---",
            StorageActionsContributor.UPLOAD_FILES,
            StorageActionsContributor.DOWNLOAD_FILE,
            "---",
            StorageActionsContributor.COPY_FILE_URL,
            StorageActionsContributor.COPY_FILE_SAS_URL,
            "---",
            StorageActionsContributor.DELETE_DIRECTORY
        );
        am.registerGroup(DIRECTORY_ACTIONS, dirActionGroup);

        final ActionGroup containerActionGroup = new ActionGroup(
            StorageActionsContributor.REFRESH,
            StorageActionsContributor.OPEN_AZURE_STORAGE_EXPLORER,
            "---",
            StorageActionsContributor.CREATE_BLOB,
            StorageActionsContributor.UPLOAD_FILES,
            StorageActionsContributor.UPLOAD_FOLDER,
            "---",
            StorageActionsContributor.COPY_FILE_URL,
            StorageActionsContributor.COPY_FILE_SAS_URL,
            "---",
            StorageActionsContributor.DELETE
        );
        am.registerGroup(CONTAINER_ACTIONS, containerActionGroup);

        final ActionGroup shareActionGroup = new ActionGroup(
            StorageActionsContributor.REFRESH,
            StorageActionsContributor.OPEN_AZURE_STORAGE_EXPLORER,
            "---",
            StorageActionsContributor.CREATE_FILE,
            StorageActionsContributor.CREATE_DIRECTORY,
            StorageActionsContributor.UPLOAD_FILES,
            StorageActionsContributor.UPLOAD_FOLDER,
            "---",
            StorageActionsContributor.COPY_FILE_URL,
            StorageActionsContributor.COPY_FILE_SAS_URL,
            "---",
            StorageActionsContributor.DELETE
        );
        am.registerGroup(SHARE_ACTIONS, shareActionGroup);

        final ActionGroup queueActionGroup = new ActionGroup(
            StorageActionsContributor.OPEN_AZURE_STORAGE_EXPLORER,
            "---",
            StorageActionsContributor.DELETE
        );
        am.registerGroup(QUEUE_ACTIONS, queueActionGroup);

        final ActionGroup tableActionGroup = new ActionGroup(
            StorageActionsContributor.OPEN_AZURE_STORAGE_EXPLORER,
            "---",
            StorageActionsContributor.DELETE
        );
        am.registerGroup(TABLE_ACTIONS, tableActionGroup);

        final IActionGroup group = am.getGroup(ResourceCommonActionsContributor.RESOURCE_GROUP_CREATE_ACTIONS);
        group.addAction(GROUP_CREATE_ACCOUNT);
    }

    @Override
    public int getOrder() {
        return INITIALIZE_ORDER;
    }

    public static void copyContentToClipboard(final String content) {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(content), null);
    }
}

/*
 * Copyright (C) 2023 Glencoe Software, Inc. All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package com.glencoesoftware.omero;

import java.util.List;

import org.slf4j.LoggerFactory;

import Ice.ObjectNotExistException;
import ome.services.blitz.repo.ManagedImportProcessI;
import ome.services.blitz.repo.ProcessContainer;
import omero.cmd.HandlePrx;
import omero.cmd.Status;

/**
 * Steward for {@link ProcessContainer} who is periodically asked to perform
 * clean up duties.
 */
public class ProcessContainerSteward implements Runnable {

    private static final org.slf4j.Logger log =
            LoggerFactory.getLogger(ProcessContainerSteward.class);

    private final ProcessContainer processContainer;

    public ProcessContainerSteward(ProcessContainer processContainer) {
        this.processContainer = processContainer;
    }

    @Override
    public void run() {
        List<ProcessContainer.Process> processes = processContainer.listProcesses(null);
        int count = processes.size();
        log.info("Number of processes in the container: {}", count);
        for (ProcessContainer.Process p : processes) {
            ManagedImportProcessI mip = null;
            long filesetId = -1;
            try {
                mip = (ManagedImportProcessI) p;
                filesetId = mip.getFileset().getId().getValue();
                /*
                 * The ImportProcessPrx is initialized at the beginning of the import.
                 * It is valid until it is closed after verifyUpload by
                 * the ImportLibrary. This means that calling any method on
                 * the ImportProcessPrx after verifyUpload throws
                 * an ObjectNotExistException.
                 * The HandlePrx is initialized during verifyUpload, so
                 * until the upload is finished, it is null. The HandlePrx
                 * is valid from the time the upload completes until the import
                 * completes, at which time it is closed and calling its methods
                 * will throw ObjectNotExistException.
                 * If the HandlePrx is null AND the ImportProcessPrx has been closed,
                 * file upload must have failed and we should clean up the
                 * ManagedImportProcessI. We should also clean up the process
                 * once the import has fully finished (HandlePrx is not null and closed).
                 */
                // If this line doesn't throw, the upload is still in progress
                HandlePrx handle = mip.getProxy().getHandle();
                if (handle == null) {
                    log.info("Import process for fileset {} has null Handle. "
                            + "File upload in progress", filesetId);
                    continue;
                }
            } catch (ObjectNotExistException e1) {
                // ImportProcessPrx threw ObjectNotExistException - either the file import
                // failed or the import progressed past verifyUpload
                HandlePrx handle = mip.getHandle(null);
                // If the handle is null, the upload failed and we should clean up
                if (handle == null) {
                    log.info("File upload failed for fileset {}, cleaning up",
                            filesetId);
                    processContainer.removeProcess(p);
                    continue;
                }
                try {
                    // If handle.getStatus() throws ObjectNotExistException, the import
                    // is complete
                    Status status = handle.getStatus();
                    log.info("Import in progress for fileset {} step {} of {}",
                            filesetId, status.currentStep, status.steps);
                } catch (ObjectNotExistException e2) {
                    log.info("ObjectNotExistException thrown by HandlePrx, "
                            + "import is complete, "
                            + "cleaning up import process for fileset {}",
                            filesetId);
                    processContainer.removeProcess(p);
                }
            } catch (Exception e) {
                log.error("Unexpected exception", e);
            }
        }
    }

}

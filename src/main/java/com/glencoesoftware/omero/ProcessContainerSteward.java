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
        log.info("Process container: {}", processContainer);
    }

    @Override
    public void run() {
        List<ProcessContainer.Process> processes = processContainer.listProcesses(null);
        int count = processes.size();
        log.info("Number of processes in the container: {}", count);
        for (ProcessContainer.Process p : processes) {
            ManagedImportProcessI mip = null;
            try {
                mip = (ManagedImportProcessI) p;
                // If the file upload hasn't completed, this will return null
                HandlePrx handle = mip.getHandle(null);
                if (handle == null) {
                    log.debug("Import process for fileset {} has null Handle. Continuing",
                            mip.getFileset().getId().getValue());
                    continue;
                }
                // If the import is still in process, this will return a valid Status
                // But if the import is done, the server-side handle will have been
                // Cleaned up and this will throw an ObjectNotExistException
                Status status = handle.getStatus();
                log.debug("Import process for fileset {} still in progress",
                        mip.getFileset().getId().getValue());
            } catch (ObjectNotExistException e) {
                try {
                    log.debug("ObjectNotExistException thrown, cleaning up "
                            + "import process for fileset {}",
                            mip.getFileset().getId().getValue());
                } catch (Exception exc) {
                    log.error("Unexpected exception getting the fileset id from the "
                            + "import process", exc);
                }
                processContainer.removeProcess(p);
            } catch (Exception e) {
                log.error("Unexpected exception", e);
            }
        }
    }

}

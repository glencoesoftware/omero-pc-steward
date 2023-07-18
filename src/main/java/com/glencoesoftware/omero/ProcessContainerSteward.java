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
            try {
                ManagedImportProcessI mip = (ManagedImportProcessI) p;
                // NOTE: If the import is in process, this getHandle will work
                // but will often/always return null; If the import has completed,
                // getHandle will throw ObjectNotExistException
                HandlePrx handle = mip.getHandle(null);
            } catch (ObjectNotExistException e) {
                log.info("ObjectNotExistException thrown by process.getHandle,"
                        + " cleaning up process");
                processContainer.removeProcess(p);
            } catch (Exception e) {
                log.error("Unexpected exception", e);
            }
        }
    }

}

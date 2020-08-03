/*
 * Copyright 2011, 2012 Guenther Hoelzl
 * <p>
 * This file is part of MINDdroid.
 * <p>
 * MINDdroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * MINDdroid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with MINDdroid.  If not, see <http://www.gnu.org/licenses/>.
 **/

package com.lego.minddroid;

public interface UploadThreadListener {

    /**
     * This will be called by the UploadThread to signal an update of the 
     * current status. 
     * @param status The current state of the UploadThread
     */
    void handleUploadThreadUpdate(final int status);
}

/**
 *   Copyright 2010 Guenther Hoelzl
 *
 *   This file is part of lejosMINDdroid.
 *
 *   lejosMINDdroid is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   lejosMINDdroid is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with lejosMINDdroid.  If not, see <http://www.gnu.org/licenses/>.
**/

/**
 * This interface has to be implemented by applications
 * connecting to MINDdroid via the MINDdroidConnector, so
 * is able to call a special command.
 */
interface CommandPerformer {
    /**
     * Performs a special command, defined via constants and
     * also delivers the needed parameters from LCP
     * @param commandNr the index of the command
     * @param parameter the LCP message array
     */
    public void performCommand(int commandNr, byte[] parameter);
}

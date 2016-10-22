/*
 * This file is part of TrackWorkTime (TWT).
 * 
 * TWT is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * TWT is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with TWT. If not, see <http://www.gnu.org/licenses/>.
 */
package org.zephyrsoft.trackworktime.backup;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;

import android.annotation.TargetApi;
import android.app.backup.BackupDataInputStream;
import android.app.backup.BackupDataOutput;
import android.app.backup.BackupHelper;
import android.content.Context;
import android.os.Build;
import android.os.ParcelFileDescriptor;

import org.zephyrsoft.trackworktime.database.DAO;

/**
 * @author Peter Rosenberg
 */
@TargetApi(Build.VERSION_CODES.FROYO)
public class DbBackupHelper implements BackupHelper {
	private static final String KEY = "db_key";
	private final Context context;
	private final WorkTimeTrackerBackupManager backupManager;

	public DbBackupHelper(Context context) {
		this.context = context;
		this.backupManager = new WorkTimeTrackerBackupManager(context);
	}

	@Override
	public void performBackup(final ParcelFileDescriptor oldState, final BackupDataOutput data,
		final ParcelFileDescriptor newState) {
		// delete backup if not enabled
		if (!backupManager.isEnabled()) {
			try {
				data.writeEntityHeader(KEY, -1); // delete existing data if any
				writeNewState(0, newState);
			} catch (IOException e) {
				// ignored, delete data next time
			}
			return;
		}

		// Get the oldState input stream
		final FileInputStream instream = new FileInputStream(oldState.getFileDescriptor());
		final DataInputStream in = new DataInputStream(instream);

		// Get the last modified timestamp from the state file and data file
		long stateModified = -1;
		try {
			stateModified = in.readLong();
			in.close();
		} catch (IOException e1) {
			// Unable to read state file... be safe and do a backup
		}
		final DAO dao = new DAO(context);
		long fileModified = dao.getLastDbModification();

		if (stateModified != fileModified) {
			ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			final Writer writer = new OutputStreamWriter(byteArrayOutputStream);

			try {
				dao.backupToWriter(writer);
				writer.close();

				data.writeEntityHeader(KEY, byteArrayOutputStream.size());
				data.writeEntityData(byteArrayOutputStream.toByteArray(),
					byteArrayOutputStream.size());
				byteArrayOutputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		writeNewState(dao.getLastDbModification(), newState);
		dao.close();
	}

	private void writeNewState(final long dbFileModification, final ParcelFileDescriptor newState) {
		// write to newState
		final FileOutputStream newStateOS = new FileOutputStream(newState.getFileDescriptor());
		final DataOutputStream newStateDataOS = new DataOutputStream(newStateOS);
		try {
			newStateDataOS.writeLong(dbFileModification);
			new WorkTimeTrackerBackupManager(context).setLastBackupTimestamp(dbFileModification);
			newStateDataOS.close();
		} catch (IOException e) {
			// error on writing the newState, ignored
		}
	}

	@Override
	public void restoreEntity(final BackupDataInputStream data) {
		final DAO dao = new DAO(context);

		if (KEY.equals(data.getKey())) {
			final BufferedReader reader = new BufferedReader(new InputStreamReader(data));

			try {
				dao.restoreFromReader(reader);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		dao.close();
	}

	@Override
	public void writeNewStateDescription(final ParcelFileDescriptor newState) {
		final DAO dao = new DAO(context);

		// write to newState
		final FileOutputStream newStateOS = new FileOutputStream(newState.getFileDescriptor());
		final DataOutputStream newStateDataOS = new DataOutputStream(newStateOS);
		try {
			newStateDataOS.writeLong(dao.getLastDbModification());
			newStateDataOS.close();
		} catch (IOException e) {
			// error on writing the newState, ignored
		}

		dao.close();
	}

}


package net.schmizz.sshj.sftp;

import androidx.annotation.NonNull;

import net.schmizz.concurrent.Promise;

import java.io.IOException;

public class RemoteFileAccessor {
    private RemoteFileAccessor() {}

    @NonNull
    public static Promise<Response, SFTPException> asyncRead(@NonNull RemoteFile file, long offset,
                                                             int length) throws IOException {
        return file.asyncRead(offset, length);
    }

    @NonNull
    public static SFTPEngine getRequester(@NonNull RemoteFile file) {
        return file.requester;
    }
}

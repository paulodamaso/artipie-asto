/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 artipie.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.artipie.asto.fs;

import com.artipie.asto.ByteArray;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.vertx.core.file.OpenOptions;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.core.file.FileSystem;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * The reactive file allows you to perform read and write operations via {@link RxFile#flow()}
 * and {@link RxFile#save(Flowable)} methods respectively.
 * <p>
 * The implementation is based on Vert.x {@link io.vertx.reactivex.core.file.AsyncFile}.
 *
 * @since 0.12
 */
public class RxFile {

    /**
     * Default size of save buffer.
     */
    private static final int SAVE_BUFF_SIZE = 1024 * 8;

    /**
     * The file location of file system.
     */
    private final Path file;

    /**
     * The file system.
     */
    private final FileSystem fls;

    /**
     * Ctor.
     * @param file The wrapped file.
     */
    public RxFile(final Path file) {
        this(file, Vertx.vertx().fileSystem());
    }

    /**
     * Ctor.
     * @param file The wrapped file.
     * @param fls The file system.
     */
    public RxFile(final Path file, final FileSystem fls) {
        this.file = file;
        this.fls = fls;
    }

    /**
     * Read file content as a flow of bytes.
     * @return A flow of bytes
     */
    public Flowable<Byte> flow() {
        return this.fls.rxOpen(this.file.toString(), new OpenOptions().setRead(true))
            .flatMapPublisher(
                asyncFile -> asyncFile.toFlowable().flatMap(
                    buffer -> Flowable.fromArray(
                        new ByteArray(buffer.getBytes()).boxedBytes()
                    )
                )
            );
    }

    /**
     * Save a flow of bytes to a file.
     * @param flow The flow of bytes
     * @return Completion or error signal
     */
    public Completable save(final Flowable<Byte> flow) {
        final int delay = 10;
        return this.fls.rxOpen(this.file.toString(), new OpenOptions().setWrite(true))
            .flatMapCompletable(
                asyncFile -> Completable.create(
                    emitter -> flow.buffer(RxFile.SAVE_BUFF_SIZE)
                        .map(bytes -> Buffer.buffer(new ByteArray(bytes).primitiveBytes()))
                        .subscribe(asyncFile.toSubscriber().onComplete(emitter::onComplete))
                )
            ).delay(delay, TimeUnit.MILLISECONDS);
    }
}
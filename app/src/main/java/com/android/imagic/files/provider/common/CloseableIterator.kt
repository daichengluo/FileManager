package com.android.imagic.files.provider.common

import java.io.Closeable

interface CloseableIterator<T> : Iterator<T>, Closeable

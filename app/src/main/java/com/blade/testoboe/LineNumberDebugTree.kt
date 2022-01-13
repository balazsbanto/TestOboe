package com.blade.testoboe

import timber.log.Timber

open class LineNumberDebugTree : Timber.DebugTree() {
    override fun createStackElementTag(element: StackTraceElement): String? {
        return "(${element.fileName}:${element.lineNumber}) #${element.methodName}"
    }
}
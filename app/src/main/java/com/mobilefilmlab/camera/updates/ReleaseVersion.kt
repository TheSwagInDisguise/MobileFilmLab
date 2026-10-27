package com.mobilefilmlab.camera.updates

data class ReleaseVersion(val major: Int,val minor: Int,val patch: Int,val stage: Int,val number: Int) : Comparable<ReleaseVersion> {
    override fun compareTo(other: ReleaseVersion): Int {
        val a=listOf(major,minor,patch,stage,number); val b=listOf(other.major,other.minor,other.patch,other.stage,other.number)
        for(i in a.indices) if(a[i]!=b[i]) return a[i].compareTo(b[i])
        return 0
    }
    companion object {
        fun parse(tag: String): ReleaseVersion? {
            val match=Regex("^v?(\\d{1,5})\\.(\\d{1,5})\\.(\\d{1,5})(?:-(alpha|beta|rc)\\.(\\d{1,5}))?$").matchEntire(tag) ?: return null
            val g=match.groupValues
            return ReleaseVersion(g[1].toInt(),g[2].toInt(),g[3].toInt(),when(g[4]) { "alpha"->0; "beta"->1; "rc"->2; else->3 },g[5].toIntOrNull() ?: 0)
        }
    }
}

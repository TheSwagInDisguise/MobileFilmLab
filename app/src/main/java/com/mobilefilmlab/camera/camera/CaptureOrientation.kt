package com.mobilefilmlab.camera.camera

object CaptureOrientation {
    fun jpeg(sensor: Int,device: Int,front: Boolean=false): Int {
        val rounded=if(device<0) 0 else ((device+45)/90*90)%360
        return (sensor+(if(front) -rounded else rounded)+360)%360
    }
}

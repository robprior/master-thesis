#version 310 es
#extension  GL_NV_shader_atomic_float : require


layout (local_size_x = 8, local_size_y = 8) in;

struct KMeansCentersStruct {
    float x; // x/y will be accumulated based on position so can go out of range of 4byte ints
    float y;
    float numPts; // this might not go over but not a big deal having float over int
    float tempX;
    float tempY;
    uint  sem;
    float closestDist;
    float closestContourX;
    float closestContourY;
    uint isActive; // boolean
    //TODO CPU?
    float gestLastX;
    float gestLastY;
};

layout(shared, binding = 2) coherent buffer KMeansCenters {
    KMeansCentersStruct centers[];
} kmeanCenter;

struct Moments {
    float m00;
    float m01;
    float m10;
    //float m11;
    //float m02;
    //float m20;
    float cogX;
    float cogY;
    float lastX;
    float lastY;
    //float mu20Prime;
    //float mu02Prime;
    //float mu11Prime;
    //float angle;
    //float slope;
    //float yIntercept;
    //float perpSlope;
    //float perpYIntercept;
};

layout(shared, binding = 5) coherent buffer MomentBuffer {
    Moments data;
} moments;


uniform int numKCenters;
uniform float distThresh;

void updateCenter(in int i) {
    float newX = kmeanCenter.centers[i].tempX / kmeanCenter.centers[i].numPts;
    float newY = kmeanCenter.centers[i].tempY / kmeanCenter.centers[i].numPts;
    //TODO can this be salvaged?
    //float centerDist = sqrt(newX -  moments.data.cogX)*(newX -  moments.data.cogX) + (newY - moments.data.cogY)*(newY - moments.data.cogY);
    //centerDist < distThresh)
    //TODO threshold needs a slider
    float amountMoved = sqrt( (newX - kmeanCenter.centers[i].x)*(newX - kmeanCenter.centers[i].x) + (newY - kmeanCenter.centers[i].y)*(newY - kmeanCenter.centers[i].y) );
    // TODO put this in to prevent large jumps to (incorrect) locations this resulted in points getting stuck
    // can't convert uint to bool this reads numPts > 5 and either it hasn't moved much or its currently inactive
    if (kmeanCenter.centers[i].numPts > 5.0){//} && (amountMoved < 125.0 || kmeanCenter.centers[i].isActive == 0u)) {
        kmeanCenter.centers[i].x      = newX;
        kmeanCenter.centers[i].y      = newY;
        kmeanCenter.centers[i].numPts = 0.0;
    } else {
        kmeanCenter.centers[i].numPts = -1.0;
    }
    kmeanCenter.centers[i].tempX           = 0.0;
    kmeanCenter.centers[i].tempY           = 0.0;
    //kmeanCenter.centers[i].sem             = 0u;
    //kmeanCenter.centers[i].closestContourX = 0.0;
    //kmeanCenter.centers[i].closestContourY = 0.0;
    //kmeanCenter.centers[i].closestDist     = 100000.0;
}

void main() {
    ivec2 pos = ivec2(gl_GlobalInvocationID.xy);

    /*
    if (pos == ivec2(0,0)) {
        // TODO this doesn't compile for certain #s worked for 9 but not 5 works for paramter but not constant FML
        /*
        for (int i = 0; i< 9; ++i) {
            kmeanCenter.centers[i].x      = kmeanCenter.centers[i].x / kmeanCenter.centers[i].numPts;
            kmeanCenter.centers[i].y      = kmeanCenter.centers[i].y / kmeanCenter.centers[i].numPts;
            kmeanCenter.centers[i].numPts = 0.0;
        }

        updateCenter(0);
        updateCenter(1);
        updateCenter(2);
        updateCenter(3);
        updateCenter(4);
    }
    */
    if (pos.x < 5 && pos.y == 0) {
        updateCenter(pos.x);
    }
}

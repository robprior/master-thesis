#version 310 es
#extension  GL_NV_shader_atomic_float : require

//TODO this entire thing might be faster in java compared to this

layout (local_size_x = 8, local_size_y = 8) in;

struct KMeansCentersStruct {

    float x;
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

struct GestureStruct {
    float centerCOGX;
    float centerCOGY;
    float numActiveCenters;
    //TODO this sufficient for scale?
    float avgDistToCenter;
    float angle;
    float xSum;
    float ySum;
};

layout(shared, binding = 4) coherent buffer GestureData {
    GestureStruct data;
} gesture;


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

void checkCenter(int id) {
    if (kmeanCenter.centers[id].isActive == 1u) {
        gesture.data.centerCOGX += kmeanCenter.centers[id].x;
        gesture.data.centerCOGY += kmeanCenter.centers[id].y;
        gesture.data.numActiveCenters += 1.0;
    }
}

void distToCenter(int id) {
    if (kmeanCenter.centers[id].isActive == 1u) {
        float x = (kmeanCenter.centers[id].x - gesture.data.centerCOGX);
        float y = (kmeanCenter.centers[id].y - gesture.data.centerCOGY);
        gesture.data.xSum += kmeanCenter.centers[id].x - moments.data.cogX;
        gesture.data.ySum += kmeanCenter.centers[id].y - moments.data.cogY;
        gesture.data.avgDistToCenter += sqrt(x*x+y*y);

        // find dot product between (x,y) and last (X,Y) -> angle average that shit
        float magNew = sqrt(x*x + y*y);
        float magOld = sqrt(kmeanCenter.centers[id].gestLastX*kmeanCenter.centers[id].gestLastX +
                            kmeanCenter.centers[id].gestLastY*kmeanCenter.centers[id].gestLastY);
        gesture.data.angle += acos((x*kmeanCenter.centers[id].gestLastX + y*kmeanCenter.centers[id].gestLastY)/
                                   (magNew * magOld));


        kmeanCenter.centers[id].gestLastX = x;
        kmeanCenter.centers[id].gestLastY = y;
    }
}

void main() {
    ivec2 pos = ivec2(gl_GlobalInvocationID.xy);

    if (pos == ivec2(0,0)) {
        gesture.data.centerCOGX = 0.0;
        gesture.data.centerCOGY = 0.0;
        gesture.data.numActiveCenters = 0.0;
        gesture.data.avgDistToCenter = 0.0;
        gesture.data.angle = 0.0;
        gesture.data.xSum = 0.0f;
        gesture.data.ySum = 0.0f;

        // I hate this so much
        //TODO (44) : error C5025: lvalue in array access too complex
        //TODO (44) : error C1068: ... or possible array index out of bounds
        /*
        for(int i = 0; i<5; ++i) {
            if (kmeanCenter.centers[i].isActive == 1u) {
                gesture.data.centerCOGX += kmeanCenter.centers[i].x;
                gesture.data.centerCOGY += kmeanCenter.centers[i].y;
                gesture.data.numActiveCenters += 1.0;
            }
        }*/
        checkCenter(0);
        checkCenter(1);
        checkCenter(2);
        checkCenter(3);
        checkCenter(4);
        // will auto just be set to 0 if numActiveCenters is 0
        gesture.data.centerCOGX /= gesture.data.numActiveCenters;
        gesture.data.centerCOGY /= gesture.data.numActiveCenters;
        // find avg dist to center
        distToCenter(0);
        distToCenter(1);
        distToCenter(2);
        distToCenter(3);
        distToCenter(4);
        //TODO this probably doesn't need sqrt / avg since just comparing ratios
        gesture.data.avgDistToCenter /= gesture.data.numActiveCenters;
        gesture.data.xSum /= gesture.data.numActiveCenters;
        gesture.data.ySum /= gesture.data.numActiveCenters;
        gesture.data.angle = atan(gesture.data.ySum, gesture.data.xSum);
    }
}

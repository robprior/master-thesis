#version 310 es


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


layout(shared, binding = 0) coherent buffer MomentBuffer {
    Moments data;
} moments;

layout (local_size_x = 8, local_size_y = 8) in;

void main() {
    ivec2 pos = ivec2(gl_GlobalInvocationID.xy);


    if (pos == ivec2(0,0)) {
        float xMean = moments.data.m10 / moments.data.m00;
        float yMean = moments.data.m01 / moments.data.m00;
        moments.data.cogX           = xMean;
        moments.data.cogY           = yMean;
        //moments.data.mu20Prime      = moments.data.m20 / moments.data.m00 - (xMean * xMean);
        //moments.data.mu02Prime      = moments.data.m02 / moments.data.m00 - (yMean * yMean);
        //moments.data.mu11Prime      = moments.data.m11 / moments.data.m00 - (xMean * yMean);
        //moments.data.angle          = atan(2.0 * moments.data.m11, (moments.data.m20 - moments.data.m02))/2.0;
        ////moments.data.angle          = atan(2.0 * moments.data.mu11Prime, (moments.data.mu20Prime - moments.data.mu02Prime))/2.0;
        //moments.data.slope          = tan(moments.data.angle);
        //moments.data.yIntercept     = moments.data.cogY - (moments.data.slope * moments.data.cogX);
        //moments.data.perpSlope      = -1.0/moments.data.slope;
        //moments.data.perpYIntercept = 0.0; //TODO remove this all slopes are the same but this changes based on pos
    }
}

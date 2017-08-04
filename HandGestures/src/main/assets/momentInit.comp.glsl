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


    /*
    if (pos == ivec2(0,0)) {
        moments.data.m10            = 0.0;
        moments.data.m01            = 0.0;
        moments.data.m00            = 0.0;
        moments.data.m11            = 0.0;
        moments.data.m02            = 0.0;
        moments.data.m20            = 0.0;
        moments.data.cogX           = 0.0;
        moments.data.cogY           = 0.0;
        moments.data.mu20Prime      = 0.0;
        moments.data.mu02Prime      = 0.0;
        moments.data.mu11Prime      = 0.0;
        moments.data.angle          = 0.0;
        moments.data.slope          = 0.0;
        moments.data.yIntercept     = 0.0;
        moments.data.perpSlope      = 0.0;
        moments.data.perpYIntercept = 0.0;
    }*/
    /*
    if (pos.x < 5 && pos.y ==0) {
        switch (pos.x) {
        case 0:
            moments.data.m10            = 0.0;
            moments.data.m01            = 0.0;
            moments.data.m00            = 0.0;
            break;
        case 1:
            moments.data.m11            = 0.0;
            moments.data.m02            = 0.0;
            moments.data.m20            = 0.0;
            break;
        case 2:
            moments.data.cogX           = 0.0;
            moments.data.cogY           = 0.0;
            moments.data.mu20Prime      = 0.0;
            break;
        case 3:
            moments.data.mu02Prime      = 0.0;
            moments.data.mu11Prime      = 0.0;
            moments.data.angle          = 0.0;
            break;
        case 4:
            moments.data.slope          = 0.0;
            moments.data.yIntercept     = 0.0;
            moments.data.perpSlope      = 0.0;
            moments.data.perpYIntercept = 0.0;
            break;
        }
    }*/
    if (pos == ivec2(0,0)) {
        moments.data.m00  = 0.0;
        moments.data.m01  = 0.0;
        moments.data.m10  = 0.0;
        moments.data.cogX = 0.0;
        moments.data.cogY = 0.0;
    }
}

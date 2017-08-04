#version 310 es

precision lowp image2D;
layout(binding = 1, rgba32f) uniform  image2D framebuffer;


struct CodeElement {
    float Ihigh;
    float Ilow;
    float min;
    float max;
    float tLast;
    float stale;
};

uniform uint numCE;

uniform float MAX_MOD;
uniform float MIN_MOD;

layout(shared, binding = 0) coherent buffer CodeBookBuffer {
    CodeElement data[];
} codeBook;


layout (local_size_x = 8, local_size_y = 8) in;

void reInitCodeElement(uint offset) {
    codeBook.data[offset].Ihigh = 0.0;
    codeBook.data[offset].Ilow  = 0.0;
    codeBook.data[offset].min   = 0.0;
    codeBook.data[offset].max   = 0.0;
    codeBook.data[offset].tLast = 0.0;
    codeBook.data[offset].stale = 0.0;
}

void mergeCodeElements(uint updatedOffset, uint deletedOffset) {
    codeBook.data[updatedOffset].tLast += codeBook.data[deletedOffset].tLast; // combine the usage #s

    codeBook.data[updatedOffset].min = min(codeBook.data[updatedOffset].min, codeBook.data[deletedOffset].min);
    codeBook.data[updatedOffset].max = max(codeBook.data[updatedOffset].max, codeBook.data[deletedOffset].max);

    codeBook.data[updatedOffset].Ilow  = min(codeBook.data[updatedOffset].Ilow , codeBook.data[deletedOffset].Ilow);
    codeBook.data[updatedOffset].Ihigh = min(codeBook.data[updatedOffset].Ihigh, codeBook.data[deletedOffset].Ihigh);

    //finnaly reitinialize
    reInitCodeElement(deletedOffset);
}

void main() {
    ivec2 pos = ivec2(gl_GlobalInvocationID.xy);

    uint width  = gl_WorkGroupSize.x * gl_NumWorkGroups.x;
    uint height = gl_WorkGroupSize.y * gl_NumWorkGroups.y;

    uint offset = uint(pos.x * int(height) + pos.y) * uint(numCE);

    uint i, j;
    for (i = uint(0); i<numCE; ++i) {
        // compare max and min to the others
        for (j = uint(0); j<numCE; ++j) {
            //check for overlap

            if (codeBook.data[offset+i].Ilow  <  codeBook.data[offset+j].Ilow &&
                codeBook.data[offset+i].Ihigh >= codeBook.data[offset+j].Ilow) {
                // case low i is less than low j
                // to be overlapping the high i must be at least as much as the low j
                mergeCodeElements(offset+i, offset+j);
            } else if(codeBook.data[offset+i].Ilow >  codeBook.data[offset+j].Ilow &&
                      codeBook.data[offset+i].Ilow <  codeBook.data[offset+j].Ihigh ) {
                // case low i is between low j and high j (already know the regions overlap)
                mergeCodeElements(offset+i, offset+j);
            }

            if (codeBook.data[offset+i].min           <  codeBook.data[offset+j].min && //no point adding min mod to both
                codeBook.data[offset+i].max + MAX_MOD >= codeBook.data[offset+j].min - MIN_MOD) {
                // case low i is less than low j
                // to be overlapping the high i must be at least as much as the low j
                mergeCodeElements(offset+i, offset+j);
            } else if(codeBook.data[offset+i].min           >  codeBook.data[offset+j].min && //no point adding min mod to both
                      codeBook.data[offset+i].min - MIN_MOD <  codeBook.data[offset+j].max + MAX_MOD) {
                // case low i is between low j and high j (already know the regions overlap)
                mergeCodeElements(offset+i, offset+j);
            }

        }
    }
}

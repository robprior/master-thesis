#version 310 es

layout(shared, binding = 3) coherent buffer ArmWidthBuffer {
    float buff[];
} armWidth;

layout(shared, binding = 4) coherent buffer WristCandidateBuffer {
    float buff[];
} wristCandidate;

layout (local_size_x = 8, local_size_y = 8) in;

void main() {
    ivec2 pos = ivec2(gl_GlobalInvocationID.xy);

    // TODO perhaps not the most efficient but should cover every reasonable buffer size
    armWidth.buff[pos.x * pos.y - 1] = 0.0;
    wristCandidate.buff[pos.x * pos.y - 1] = 0.0;
}

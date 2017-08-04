#version 310 es

precision lowp image2D;
layout(binding = 1, rgba32f) uniform  image2D inputBuffer;

layout(binding = 3, rgba32f) uniform  image2D outputBuffer;

layout (local_size_x = 8, local_size_y = 8) in;

void main() {
    ivec2 pos = ivec2(gl_GlobalInvocationID.xy);

    vec4 c = imageLoad(inputBuffer, pos);

    vec4 gradIntensity = vec4(0.0);

    if (c.a != 0.0) {
        float intensity = 0.0;


        vec4 b  = imageLoad(inputBuffer, pos + ivec2( 0,-1));
        vec4 l  = imageLoad(inputBuffer, pos + ivec2(-1, 0));
        vec4 r  = imageLoad(inputBuffer, pos + ivec2( 1, 0));
        vec4 t  = imageLoad(inputBuffer, pos + ivec2( 0, 1));

        intensity +=  t.a;
        intensity +=  r.a;
        intensity +=  l.a;
        intensity +=  b.a;

        intensity = (intensity >= 4.0) ? 0.0 : intensity; // all orth neighbours are white
        intensity = (intensity <= 1.0) ? 0.0 : intensity; // only 1 or fewer neighbours

        intensity = clamp(intensity,0.0,1.0);

        gradIntensity =  vec4(intensity);
    }
    imageStore(outputBuffer, pos, gradIntensity);
}

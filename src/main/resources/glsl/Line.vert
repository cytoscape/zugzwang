#version 450
//#extension GL_ARB_bindless_texture : require

layout (location = 0) uniform mat4 viewMatrix;

layout (location = 0) in vec3 in_position;
layout (location = 1) in uvec4 in_sizes;
//layout (location = 2) in uint in_texID;
// ADD FLAT QUALIFIER TO TEXTUREID!

out VS_OUT
{
	uvec4 sizes;
	flat int texID;
} vs_out;

void main(void)
{
	vs_out.sizes = in_sizes;
	vs_out.texID = gl_VertexID / 2;
	vec4 transformed = viewMatrix * vec4(in_position, 1.0f);
	//transformed.z -= 1e-3f + 1e-4f * float(gl_VertexID);
	gl_Position = transformed;
}
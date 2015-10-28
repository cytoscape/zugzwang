#version 440
//#extension GL_ARB_bindless_texture : require

uniform mat4 projMatrix;
uniform float depthOffset;

layout (points) in;
layout (triangle_strip, max_vertices = 4) out;

in VS_OUT
{
	ivec4 sizes;
	ivec2 offset;
	flat int texID;
} gs_in[];

out GS_OUT
{
	vec2 texCoords;
	flat int texID;
} gs_out;

void main(void)
{	
	vec4 original = gl_in[0].gl_Position;
	int texID = gs_in[0].texID;
	// Invert vertical offset because OpenGL's 0 is in the bottom left corner.
	int leftWidth = -gs_in[0].sizes.x / 2 + gs_in[0].offset.x;
	int topHeight = gs_in[0].sizes.y / 2 - gs_in[0].offset.y;
	int rightWidth = (gs_in[0].sizes.x + 1) / 2 + gs_in[0].offset.x;
	int bottomHeight = -((gs_in[0].sizes.y + 1) / 2) - gs_in[0].offset.y;
	float halfPixelU = 0.5f / float(gs_in[0].sizes.z);
	float halfPixelV = 0.5f / float(gs_in[0].sizes.w);
	
	float zOffset = depthOffset;
	
	gs_out.texCoords = vec2(halfPixelU, 1.0f - halfPixelV);
	gs_out.texID = texID;
	gl_Position = projMatrix * vec4(original.x + leftWidth, original.y + bottomHeight, original.z, original.w);
	gl_Position /= gl_Position.w;
	gl_Position.z += zOffset;
	EmitVertex();
	
	gs_out.texCoords = vec2(halfPixelU, halfPixelV);
	gs_out.texID = texID;
	gl_Position = projMatrix * vec4(original.x + leftWidth, original.y + topHeight, original.z, original.w);
	gl_Position /= gl_Position.w;
	gl_Position.z += zOffset;
	EmitVertex();
	
	gs_out.texCoords = vec2(1.0f - halfPixelU, 1.0f - halfPixelV);
	gs_out.texID = texID;
	gl_Position = projMatrix * vec4(original.x + rightWidth, original.y + bottomHeight, original.z, original.w);
	gl_Position /= gl_Position.w;
	gl_Position.z += zOffset;
	EmitVertex();
	
	gs_out.texCoords = vec2(1.0f - halfPixelU, halfPixelV);
	gs_out.texID = texID;
	gl_Position = projMatrix * vec4(original.x + rightWidth, original.y + topHeight, original.z, original.w);
	gl_Position /= gl_Position.w;
	gl_Position.z += zOffset;
	EmitVertex();
}
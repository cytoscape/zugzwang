#version 450
//#extension GL_ARB_bindless_texture : require

layout (location = 1) uniform mat4 projMatrix;

layout (points) in;
layout (triangle_strip, max_vertices = 4) out;

in VS_OUT
{
	uvec4 sizes;
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
	uint leftWidth = gs_in[0].sizes.x / 2;
	uint topHeight = gs_in[0].sizes.y / 2;
	uint rightWidth = (gs_in[0].sizes.x + 1) / 2;
	uint bottomHeight = (gs_in[0].sizes.y + 1) / 2;
	float halfPixelU = 0.5f / float(gs_in[0].sizes.z);
	float halfPixelV = 0.5f / float(gs_in[0].sizes.w);
	
	/*uint leftWidth = 2;
	uint topHeight = 1;
	uint rightWidth = 2;
	uint bottomHeight = 1;*/
	
	gs_out.texCoords = vec2(0.0f + halfPixelU, 1.0f - halfPixelV);
	gs_out.texID = texID;
	gl_Position = projMatrix * vec4(original.x - leftWidth, original.y - bottomHeight, original.z, original.w);
	EmitVertex();
	
	gs_out.texCoords = vec2(0.0f + halfPixelU, 0.0f + halfPixelV);
	gs_out.texID = texID;
	gl_Position = projMatrix * vec4(original.x - leftWidth, original.y + topHeight, original.z, original.w);
	EmitVertex();
	
	gs_out.texCoords = vec2(1.0f - halfPixelU, 1.0f - halfPixelV);
	gs_out.texID = texID;
	gl_Position = projMatrix * vec4(original.x + rightWidth, original.y - bottomHeight, original.z, original.w);
	EmitVertex();
	
	gs_out.texCoords = vec2(1.0f - halfPixelU, 0.0f + halfPixelV);
	gs_out.texID = texID;
	gl_Position = projMatrix * vec4(original.x + rightWidth, original.y + topHeight, original.z, original.w);
	EmitVertex();
}
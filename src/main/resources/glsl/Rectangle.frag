#version 440
#extension GL_ARB_bindless_texture : require

layout (std140, binding = 0) buffer Samplers
{
	sampler2D handle[];
};

in GS_OUT
{
	vec2 texCoords;
	flat int texID;
} fs_in;

out vec4 color;

void main(void)
{
	//sampler2D s = sampler2D(fs_in.texID);
	//float bla = float(handle[0]) / 255.0f;
	//color = vec4(bla, bla, bla, 1.0f);
	//color = vec4(float(texID), 1.0f, 1.0f, 1.0f);
	
	color = texture(handle[fs_in.texID], fs_in.texCoords).abgr;
	if (color.a < 0.01f)
		discard;
	//color = vec4(color.rgb, 1.0f);
}
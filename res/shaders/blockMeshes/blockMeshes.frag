#version 450

in vec3 vertex;
in vec4 color;
in vec3 normal;
in vec2 texCoord;
flat in int textureId;

uniform sampler2DArray albedoTextures;

out vec4 colorBuffer;
out vec4 normalBuffer;

#include struct xyz.chunkstories.api.graphics.structs.Camera
uniform Camera camera;

#include struct xyz.chunkstories.api.graphics.structs.WorldConditions
uniform WorldConditions world;

#include ../sky/sky.glsl
#include ../normalcompression.glsl

#include struct xyz.chunkstories.api.math.random.PrecomputedSimplexSeed
uniform PrecomputedSimplexSeed simplexSeed;

#include simplex.glsl
#include noise.glsl

void main()
{
	// The magic virtual texturing stuff 
	// ( requires EXT_descriptor_indexing.shaderSampledImageArrayNonUniformIndexing )
	//vec4 albedo = vtexture2D(textureId, texCoord);
	vec4 albedo = texture(albedoTextures, vec3(texCoord, textureId));

	if(albedo.a == 0.0) {
		discard;
	}

	if(albedo.a < 1.0) {
		albedo.rgb *= vec3(0.4, 0.8, 0.4);
		albedo.a = 1.0;

	}
	
	//albedo.rgb = vec3(vertex.y / 64.0 - 1.0);
	//albedo.rgb = vec3(0.5 + 1.0 * fractalNoise(vertex.xz, 5, 1.0, 0.5));
	//albedo.rgb = vec3(fract(vertex.x) < 0.5 ? 1.0 : 0.0, fract(vertex.z) < 0.5 ? 1.0 : 0.0, mod(vertex.y, 256.0) / 256.0);

	float ao = 0.95 + 0.05 * color.z;

	ao = ao * ao;
	ao = ao * ao;
	ao = ao * ao;
	ao = ao * ao;
	//ao = 0.5 + 0.5 * ao;

	colorBuffer = albedo;
	normalBuffer = vec4(encodeNormal(normalize(normal)), vec2(color.x * ao, color.y));
}
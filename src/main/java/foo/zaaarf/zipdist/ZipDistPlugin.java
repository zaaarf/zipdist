package foo.zaaarf.zipdist;

import org.gradle.api.*;
import org.gradle.api.file.Directory;
import org.gradle.api.file.RegularFile;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.tasks.Delete;
import org.gradle.api.tasks.bundling.Zip;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Objects;

/**
 * The main class for the zipdist plugin.
 */
public class ZipDistPlugin implements Plugin<Project> {
	@Override
	public void apply(@NotNull Project project) {
		project.getPluginManager().apply("maven-publish");

		// setup android if needed
		String componentName = "java";
		if(
			project.getPluginManager().hasPlugin("com.android.application")
				|| project.getPluginManager().hasPlugin("com.android.library")
		) {
			Object androidExt = project.getExtensions().findByName("android");
			try {
				Method getPublishing = Objects.requireNonNull(androidExt).getClass().getMethod("getPublishing");
				Object publishing = getPublishing.invoke(androidExt);

				if(publishing != null) {
					Method getSingleVariants = publishing.getClass().getMethod("getSingleVariants");
					Collection<?> variants = (Collection<?>) getSingleVariants.invoke(publishing);

					if(variants.isEmpty()) {
						Method singleVariant = publishing.getClass().getMethod("singleVariant", String.class);
						singleVariant.invoke(publishing, "release");
						componentName = "release";
					} else { // user wants to do things manually
						componentName = null;
					}
				}
			} catch(Exception e) {
				throw new RuntimeException(e);
			}
		}

		String finalComponentName = componentName; // i hate java
		Directory distDir = project.getLayout().getBuildDirectory().dir("dist").get();
		Directory distRoot = distDir.dir("maven");
		project.afterEvaluate(p -> {
			p.getExtensions().configure(PublishingExtension.class, publishing -> {
				if(publishing.getPublications().isEmpty() && finalComponentName != null) {
					// if this was set to null then it means the user wants to do it manually
					publishing.publications(publications -> publications.create(
						"mavenJava",
						MavenPublication.class,
						publication -> publication.from(p.getComponents().findByName(finalComponentName)))
					);
				}

				publishing.repositories(repos -> repos.maven(mavenRepo -> {
					mavenRepo.setName("localDist");
					mavenRepo.setUrl(distRoot);
				}));
			});

			project.getTasks().register("cleanOldDist", Delete.class, d -> d.delete(distDir));
			project.getTasks().getByName("publishAllPublicationsToLocalDistRepository").dependsOn("cleanOldDist");
		});

		RegularFile zipFile = distDir.dir("output").file("dist.zip");
		project.getTasks().register("zipDist", Zip.class, z -> {
			z.getOutputs().upToDateWhen(t -> false);
			z.dependsOn("publishAllPublicationsToLocalDistRepository");
			z.from(distRoot);
			z.getDestinationDirectory().set(zipFile.getAsFile().getParentFile());
			z.getArchiveFileName().set(zipFile.getAsFile().getName());
		});
	}
}

package foo.zaaarf.zipdist;

import org.gradle.api.*;
import org.gradle.api.file.Directory;
import org.gradle.api.file.RegularFile;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.tasks.Delete;
import org.gradle.api.tasks.bundling.Zip;
import org.jetbrains.annotations.NotNull;

public class ZipDistPlugin implements Plugin<Project> {
	@Override
	public void apply(@NotNull Project project) {
		project.getPluginManager().apply("maven-publish");

		Directory distDir = project.getLayout().getBuildDirectory().dir("dist").get();
		Directory distRoot = distDir.dir("maven");

		project.getExtensions().configure(PublishingExtension.class, publishing -> {
			publishing.publications(publications -> {
				publications.create("mavenJava", MavenPublication.class, publication -> {
					publication.from(project.getComponents().findByName("java"));
				});
			});

			publishing.repositories(repos -> {
				repos.maven(mavenRepo -> {
					mavenRepo.setName("localDist");
					mavenRepo.setUrl(distRoot);
				});
			});
		});

		project.getTasks().register("cleanOldDist", Delete.class, d -> d.delete(distDir));
		project.getTasks().getByName("publishAllPublicationsToLocalDistRepository").dependsOn("cleanOldDist");

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

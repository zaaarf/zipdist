# ZipDist
A simple gradle plugin that provides a `zipDist` task that you can execute to get a zipped version of its Maven publication.

A zip obtained this way can be placed in the root folder of your Maven server and unzipped, and it will put everything
in the right place.

This is published on Maven Central, so just add the plugin `foo.zaaarf.zipdist`.

This was developed for my personal and very specific use case, so I don't expect it 
to be resilient across different setups.

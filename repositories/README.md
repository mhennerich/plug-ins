# Project to generate the p2 repository.

Since the archives are not deployed, the SNAPSHOT qualifier in the
archive name is not substituted,
and must be processed externally by the publish script. (using
`${maven.build.timestamp}` is not ok, since it is captured when
the entire build starts, which is before the moment when plug-ins
are internally deployed to the repository).

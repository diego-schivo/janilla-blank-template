package com.janilla.blanktemplate.backend;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;

import com.janilla.backend.cms.CmsResourceHandling;
import com.janilla.backend.persistence.CrudObserver;
import com.janilla.blanktemplate.Media;

public class MediaCrudObserver implements CrudObserver<Media> {

	protected final CmsResourceHandling handling;

	public MediaCrudObserver(CmsResourceHandling cmsResourceHandling) {
		this.handling = cmsResourceHandling;
	}

	@Override
	public void afterDelete(Media entity) {
		var n = entity.file() != null ? entity.file().name() : null;
		var f = n != null ? handling.directory().resolve(n) : null;
		if (f != null && Files.exists(f))
			try {
				Files.delete(f);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
	}
}

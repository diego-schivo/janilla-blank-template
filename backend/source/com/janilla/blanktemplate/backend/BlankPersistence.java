package com.janilla.blanktemplate.backend;

import java.util.List;

import com.janilla.backend.cms.CmsPersistence;
import com.janilla.backend.persistence.Crud;
import com.janilla.backend.persistence.CrudObserver;
import com.janilla.backend.sqlite.SqliteDatabase;
import com.janilla.blanktemplate.Media;
import com.janilla.ioc.DiFactory;
import com.janilla.java.TypeResolver;
import com.janilla.persistence.Entity;

public class BlankPersistence extends CmsPersistence {

	protected final DiFactory diFactory;

	public BlankPersistence(SqliteDatabase database, List<Class<? extends Entity<?>>> storables,
			TypeResolver typeResolver, DiFactory diFactory) {
		this.diFactory = diFactory;
		super(database, storables, typeResolver);
	}

	@Override
	protected <E extends Entity<?>> Crud<?, E> newCrud(Class<E> type) {
		var c = super.newCrud(type);
		if (c != null) {
			Class<? extends CrudObserver<?>> t;
			if (type == Media.class)
				t = MediaCrudObserver.class;
			else
				t = null;
			if (t != null) {
				@SuppressWarnings("unchecked")
				var o = (CrudObserver<E>) diFactory.create(t);
				c.observers().add(o);
			}
		}
		return c;
	}
}

package com.fsck.k9.pEp.infrastructure.components;

import com.fsck.k9.activity.K9Activity;
import com.fsck.k9.pEp.infrastructure.modules.ApplicationModule;
import com.fsck.k9.pEp.infrastructure.threading.PostExecutionThread;
import com.fsck.k9.pEp.infrastructure.threading.ThreadExecutor;
import com.fsck.k9.pEp.ui.PepColoredActivity;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = ApplicationModule.class)
public interface ApplicationComponent {

    void inject(K9Activity k9Activity);

    void inject(PepColoredActivity pepColoredActivity);

    ThreadExecutor getThreadExecutor();

    PostExecutionThread getPostExecutionThread();
}

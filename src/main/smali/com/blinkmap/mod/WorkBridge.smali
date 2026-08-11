.class public final Lcom/blinkmap/mod/WorkBridge;
.super Ljava/lang/Object;

.method public static checkUsername(Landroid/content/Context;Ljava/lang/String;)V
    .locals 5
    :try_start_username
    invoke-virtual {p0}, Landroid/content/Context;->getApplicationContext()Landroid/content/Context;
    move-result-object v0
    check-cast v0, Lcom/blinkmap/App;
    invoke-virtual {v0}, Lcom/blinkmap/App;->b()Ljava/lang/Object;
    move-result-object v0
    check-cast v0, Lae6;
    invoke-virtual {v0}, Lae6;->Q()Lbyl;
    move-result-object v0
    invoke-static {v0}, Laa;->l(Lbyl;)Lhe;
    move-result-object v0
    new-instance v1, Ldb4;
    invoke-direct {v1, p1}, Ldb4;-><init>(Ljava/lang/String;)V
    new-instance v2, Lcom/blinkmap/mod/UsernameCheckContinuation;
    invoke-direct {v2}, Lcom/blinkmap/mod/UsernameCheckContinuation;-><init>()V
    invoke-interface {v0, v1, v2}, Lhe;->c(Ldb4;Lio5;)Ljava/lang/Object;
    move-result-object v3
    sget-object v4, Lwu5;->COROUTINE_SUSPENDED:Lwu5;
    if-eq v3, v4, :username_done
    invoke-static {v3}, Lcom/blinkmap/mod/UsernameCheckContinuation;->handle(Ljava/lang/Object;)V
    :try_end_username
    .catch Ljava/lang/Throwable; {:try_start_username .. :try_end_username} :username_catch
    :username_done
    return-void
    :username_catch
    move-exception v0
    invoke-static {v0}, Lcom/blinkmap/mod/StepHooks;->onUsernameCheckError(Ljava/lang/Throwable;)V
    return-void
.end method

.method public static lookupUser(Landroid/content/Context;Ljava/lang/String;)V
    .locals 4
    :try_start_user
    invoke-virtual {p0}, Landroid/content/Context;->getApplicationContext()Landroid/content/Context;
    move-result-object v0
    check-cast v0, Lcom/blinkmap/App;
    invoke-virtual {v0}, Lcom/blinkmap/App;->b()Ljava/lang/Object;
    move-result-object v0
    check-cast v0, Lae6;
    invoke-virtual {v0}, Lae6;->Q()Lbyl;
    move-result-object v0
    invoke-static {v0}, Lwq0;->s(Lbyl;)Lelk;
    move-result-object v0
    new-instance v1, Lcom/blinkmap/mod/UserLookupContinuation;
    invoke-direct {v1}, Lcom/blinkmap/mod/UserLookupContinuation;-><init>()V
    invoke-interface {v0, p1, v1}, Lelk;->b(Ljava/lang/String;Lio5;)Ljava/lang/Object;
    move-result-object v2
    sget-object v3, Lwu5;->COROUTINE_SUSPENDED:Lwu5;
    if-eq v2, v3, :user_done
    invoke-static {v2}, Lcom/blinkmap/mod/UserLookupContinuation;->handle(Ljava/lang/Object;)V
    :try_end_user
    .catch Ljava/lang/Throwable; {:try_start_user .. :try_end_user} :user_catch
    :user_done
    return-void
    :user_catch
    move-exception v0
    invoke-static {v0}, Lcom/blinkmap/mod/StepHooks;->onUserLookupError(Ljava/lang/Throwable;)V
    return-void
.end method

.method public static loadMyProfile(Landroid/content/Context;)V
    .locals 4
    :try_start_profile
    invoke-virtual {p0}, Landroid/content/Context;->getApplicationContext()Landroid/content/Context;
    move-result-object v0
    check-cast v0, Lcom/blinkmap/App;
    invoke-virtual {v0}, Lcom/blinkmap/App;->b()Ljava/lang/Object;
    move-result-object v0
    check-cast v0, Lae6;
    invoke-virtual {v0}, Lae6;->Q()Lbyl;
    move-result-object v0
    invoke-static {v0}, Laa;->l(Lbyl;)Lhe;
    move-result-object v0
    new-instance v1, Lcom/blinkmap/mod/ProfileContinuation;
    invoke-direct {v1}, Lcom/blinkmap/mod/ProfileContinuation;-><init>()V
    invoke-interface {v0, v1}, Lhe;->get(Lio5;)Ljava/lang/Object;
    move-result-object v2
    sget-object v3, Lwu5;->COROUTINE_SUSPENDED:Lwu5;
    if-eq v2, v3, :profile_done
    invoke-static {v2}, Lcom/blinkmap/mod/ProfileContinuation;->handle(Ljava/lang/Object;)V
    :try_end_profile
    .catch Ljava/lang/Throwable; {:try_start_profile .. :try_end_profile} :profile_catch
    :profile_done
    return-void
    :profile_catch
    move-exception v0
    invoke-static {v0}, Lcom/blinkmap/mod/StepHooks;->onProfileError(Ljava/lang/Throwable;)V
    return-void
.end method

.method public static loadTopFriends(Landroid/content/Context;)V
    .locals 4
    :try_start_friends
    invoke-virtual {p0}, Landroid/content/Context;->getApplicationContext()Landroid/content/Context;
    move-result-object v0
    check-cast v0, Lcom/blinkmap/App;
    invoke-virtual {v0}, Lcom/blinkmap/App;->b()Ljava/lang/Object;
    move-result-object v0
    check-cast v0, Lae6;
    invoke-virtual {v0}, Lae6;->B()Lelb;
    move-result-object v0

    new-instance v1, Lcom/blinkmap/mod/FriendListContinuation;
    invoke-direct {v1}, Lcom/blinkmap/mod/FriendListContinuation;-><init>()V
    invoke-interface {v0, v1}, Lelb;->a(Lio5;)Ljava/lang/Object;
    move-result-object v2
    sget-object v3, Lwu5;->COROUTINE_SUSPENDED:Lwu5;
    if-eq v2, v3, :friends_done
    invoke-static {v2}, Lcom/blinkmap/mod/FriendListContinuation;->handle(Ljava/lang/Object;)V
    :try_end_friends
    .catch Ljava/lang/Throwable; {:try_start_friends .. :try_end_friends} :friends_catch

    :friends_done
    return-void

    :friends_catch
    move-exception v0
    invoke-static {v0}, Lcom/blinkmap/mod/StepHooks;->onFriendsError(Ljava/lang/Throwable;)V
    return-void
.end method

.method public static searchChats(Landroid/content/Context;Ljava/lang/String;)V
    .locals 6
    :try_start_chat
    invoke-virtual {p0}, Landroid/content/Context;->getApplicationContext()Landroid/content/Context;
    move-result-object v0
    check-cast v0, Lcom/blinkmap/App;
    invoke-virtual {v0}, Lcom/blinkmap/App;->b()Ljava/lang/Object;
    move-result-object v0
    check-cast v0, Lae6;
    invoke-virtual {v0}, Lae6;->Q()Lbyl;
    move-result-object v0
    invoke-static {v0}, Ljff;->f(Lbyl;)Lkq3;
    move-result-object v0

    if-nez p1, :query_ready
    const-string p1, ""
    :query_ready
    new-instance v1, Lcom/blinkmap/mod/ChatSearchContinuation;
    invoke-direct {v1}, Lcom/blinkmap/mod/ChatSearchContinuation;-><init>()V
    const/16 v2, 0x64
    const/4 v3, 0x0
    invoke-interface {v0, v2, v3, p1, v1}, Lkq3;->b(ILgmg;Ljava/lang/String;Lio5;)Ljava/lang/Object;
    move-result-object v4
    sget-object v5, Lwu5;->COROUTINE_SUSPENDED:Lwu5;
    if-eq v4, v5, :chat_done
    invoke-static {v4}, Lcom/blinkmap/mod/ChatSearchContinuation;->handle(Ljava/lang/Object;)V
    :try_end_chat
    .catch Ljava/lang/Throwable; {:try_start_chat .. :try_end_chat} :chat_catch

    :chat_done
    return-void

    :chat_catch
    move-exception v0
    invoke-static {v0}, Lcom/blinkmap/mod/StepHooks;->onChatSearchError(Ljava/lang/Throwable;)V
    return-void
.end method

.method public static enqueueFitness(Landroid/content/Context;)V
    .locals 3

    new-instance v0, Ldsi;

    const-class v1, Lcom/blinkmap/system/fitness/FitnessUpdateWorker;

    invoke-direct {v0, v1}, Ls7r;-><init>(Ljava/lang/Class;)V

    invoke-virtual {v0}, Ls7r;->a()Lt7r;

    move-result-object v0

    check-cast v0, Lesi;

    invoke-static {p0}, Lh7r;->f(Landroid/content/Context;)Lh7r;

    move-result-object p0

    const-string v1, "worker_update_steps"

    sget-object v2, Lqu9;->REPLACE:Lqu9;

    invoke-virtual {p0, v1, v2, v0}, Lg7r;->b(Ljava/lang/String;Lqu9;Lesi;)V

    return-void
.end method

.method public static post100k(Landroid/content/Context;JLjava/lang/String;)V
    .locals 8

    :try_start
    invoke-virtual {p0}, Landroid/content/Context;->getApplicationContext()Landroid/content/Context;
    move-result-object v0
    check-cast v0, Lcom/blinkmap/App;

    invoke-virtual {v0}, Lcom/blinkmap/App;->b()Ljava/lang/Object;
    move-result-object v0
    check-cast v0, Lae6;
    invoke-virtual {v0}, Lae6;->Q()Lbyl;
    move-result-object v0
    invoke-static {v0}, Lwq0;->C(Lbyl;)Lg2o;
    move-result-object v0

    new-instance v1, Ljava/util/ArrayList;
    invoke-direct {v1}, Ljava/util/ArrayList;-><init>()V

    new-instance v3, Lf4o;
    const v4, 0x186a0
    move-wide v5, p1
    move-object v7, p3
    invoke-direct/range {v3 .. v7}, Lf4o;-><init>(IJLjava/lang/String;)V
    invoke-virtual {v1, v3}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    new-instance v2, Lc4o;
    const-string v3, "sensor"
    invoke-direct {v2, v1, v3}, Lc4o;-><init>(Ljava/util/ArrayList;Ljava/lang/String;)V

    new-instance v1, Lcom/blinkmap/mod/ApiContinuation;
    invoke-direct {v1, v0}, Lcom/blinkmap/mod/ApiContinuation;-><init>(Lg2o;)V
    invoke-interface {v0, v2, v1}, Lg2o;->a(Lc4o;Lio5;)Ljava/lang/Object;
    move-result-object v0

    sget-object v1, Lwu5;->COROUTINE_SUSPENDED:Lwu5;
    if-eq v0, v1, :done
    invoke-static {v0}, Lcom/blinkmap/mod/StepHooks;->onApiResult(Ljava/lang/Object;)V
    invoke-static {p0}, Lcom/blinkmap/mod/WorkBridge;->service(Landroid/content/Context;)Lg2o;
    move-result-object v0
    invoke-static {v0}, Lcom/blinkmap/mod/WorkBridge;->verifyDay(Lg2o;)V
    :try_end
    .catch Ljava/lang/Throwable; {:try_start .. :try_end} :catch

    :done
    return-void

    :catch
    move-exception v0
    invoke-static {v0}, Lcom/blinkmap/mod/StepHooks;->onApiError(Ljava/lang/Throwable;)V
    return-void
.end method

.method private static service(Landroid/content/Context;)Lg2o;
    .locals 1
    invoke-virtual {p0}, Landroid/content/Context;->getApplicationContext()Landroid/content/Context;
    move-result-object v0
    check-cast v0, Lcom/blinkmap/App;
    invoke-virtual {v0}, Lcom/blinkmap/App;->b()Ljava/lang/Object;
    move-result-object v0
    check-cast v0, Lae6;
    invoke-virtual {v0}, Lae6;->Q()Lbyl;
    move-result-object v0
    invoke-static {v0}, Lwq0;->C(Lbyl;)Lg2o;
    move-result-object v0
    return-object v0
.end method

.method public static verifyDay(Lg2o;)V
    .locals 3
    :try_start
    new-instance v0, Lcom/blinkmap/mod/VerifyContinuation;
    invoke-direct {v0}, Lcom/blinkmap/mod/VerifyContinuation;-><init>()V
    const-string v1, "day"
    invoke-interface {p0, v1, v0}, Lg2o;->b(Ljava/lang/String;Lio5;)Ljava/lang/Object;
    move-result-object v0
    sget-object v1, Lwu5;->COROUTINE_SUSPENDED:Lwu5;
    if-eq v0, v1, :done_verify
    check-cast v0, Lj4o;
    iget-object v1, v0, Lj4o;->a:Llpl;
    iget v1, v1, Llpl;->b:I
    invoke-static {v1}, Lcom/blinkmap/mod/StepHooks;->onApiVerified(I)V
    :try_end
    .catch Ljava/lang/Throwable; {:try_start .. :try_end} :verify_catch
    :done_verify
    return-void

    :verify_catch
    move-exception v2
    invoke-static {v2}, Lcom/blinkmap/mod/StepHooks;->onApiVerifyError(Ljava/lang/Throwable;)V
    return-void
.end method

.method public static verifyNow(Landroid/content/Context;)V
    .locals 1
    :try_start_now
    invoke-static {p0}, Lcom/blinkmap/mod/WorkBridge;->service(Landroid/content/Context;)Lg2o;
    move-result-object v0
    invoke-static {v0}, Lcom/blinkmap/mod/WorkBridge;->verifyDay(Lg2o;)V
    :try_end_now
    .catch Ljava/lang/Throwable; {:try_start_now .. :try_end_now} :catch_now
    return-void

    :catch_now
    move-exception v0
    invoke-static {v0}, Lcom/blinkmap/mod/StepHooks;->onApiVerifyError(Ljava/lang/Throwable;)V
    return-void
.end method

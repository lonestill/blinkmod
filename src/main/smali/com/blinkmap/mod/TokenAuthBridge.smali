.class public final Lcom/blinkmap/mod/TokenAuthBridge;
.super Ljava/lang/Object;
.source "TokenAuthBridge.java"


# direct methods
.method private constructor <init>()V
    .locals 0

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method

.method public static check(Landroid/app/Activity;)V
    .locals 4

    :try_start_0
    sget-object v0, Lcym;->j:Lcym;

    if-nez v0, :cond_0

    new-instance v0, Lcym;

    invoke-direct {v0, p0}, Lcym;-><init>(Landroid/content/Context;)V

    :cond_0
    new-instance v1, Lcom/blinkmap/mod/TokenSessionCheckContinuation;

    invoke-direct {v1, p0}, Lcom/blinkmap/mod/TokenSessionCheckContinuation;-><init>(Landroid/app/Activity;)V

    invoke-virtual {v0, v1}, Lcym;->h(Ljo5;)Ljava/lang/Object;

    move-result-object v2

    sget-object v3, Lwu5;->COROUTINE_SUSPENDED:Lwu5;

    if-eq v2, v3, :cond_1

    invoke-static {p0, v2}, Lcom/blinkmap/mod/TokenSessionCheckContinuation;->handle(Landroid/app/Activity;Ljava/lang/Object;)V

    goto :goto_0

    invoke-static {p0}, Lcom/blinkmap/mod/TokenLoginOverlay;->onSessionCheckFailed(Landroid/app/Activity;)V
    :try_end_0
    .catch Ljava/lang/Throwable; {:try_start_0 .. :try_end_0} :catch_0

    :cond_1
    :goto_0
    return-void

    :catch_0
    move-exception v0

    invoke-static {p0}, Lcom/blinkmap/mod/TokenLoginOverlay;->onSessionCheckFailed(Landroid/app/Activity;)V

    return-void
.end method

.method public static save(Landroid/app/Activity;Ljava/lang/String;J)V
    .locals 5

    :try_start_0
    sget-object v0, Lcym;->j:Lcym;

    if-nez v0, :cond_0

    new-instance v0, Lcym;

    invoke-direct {v0, p0}, Lcym;-><init>(Landroid/content/Context;)V

    :cond_0
    new-instance v1, Led;

    invoke-direct {v1, p2, p3}, Led;-><init>(J)V

    new-instance v2, Lcom/blinkmap/mod/TokenAccountContinuation;

    invoke-direct {v2, p0, v0, p1}, Lcom/blinkmap/mod/TokenAccountContinuation;-><init>(Landroid/app/Activity;Lcym;Ljava/lang/String;)V

    invoke-virtual {v0, v1, v2}, Lcym;->i(Led;Ljo5;)Ljava/lang/Object;

    move-result-object v3

    sget-object v4, Lwu5;->COROUTINE_SUSPENDED:Lwu5;

    if-eq v3, v4, :cond_1

    invoke-static {v2, v3}, Lcom/blinkmap/mod/TokenAccountContinuation;->handle(Lcom/blinkmap/mod/TokenAccountContinuation;Ljava/lang/Object;)V

    goto :goto_0

    invoke-static {p0}, Lcom/blinkmap/mod/TokenLoginOverlay;->onSaveFailed(Landroid/app/Activity;)V
    :try_end_0
    .catch Ljava/lang/Throwable; {:try_start_0 .. :try_end_0} :catch_0

    :cond_1
    :goto_0
    return-void

    :catch_0
    move-exception v0

    invoke-static {p0}, Lcom/blinkmap/mod/TokenLoginOverlay;->onSaveFailed(Landroid/app/Activity;)V

    return-void
.end method

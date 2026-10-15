.class public final Lcom/blinkmap/mod/TokenExportBridge;
.super Ljava/lang/Object;
.source "TokenExportBridge.java"


# direct methods
.method private constructor <init>()V
    .locals 0

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method

.method public static copy(Landroid/app/Activity;)V
    .locals 4

    :try_start_0
    sget-object v0, Lcym;->j:Lcym;

    if-nez v0, :cond_0

    const-string v1, "BlinkModToken"

    const-string v2, "Auth store singleton unavailable; creating fallback"

    invoke-static {v1, v2}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I

    new-instance v0, Lcym;

    invoke-direct {v0, p0}, Lcym;-><init>(Landroid/content/Context;)V

    :cond_0
    new-instance v1, Lcom/blinkmap/mod/TokenExportContinuation;

    invoke-direct {v1, p0}, Lcom/blinkmap/mod/TokenExportContinuation;-><init>(Landroid/app/Activity;)V

    invoke-virtual {v0, v1}, Lcym;->h(Ljo5;)Ljava/lang/Object;

    move-result-object v2

    sget-object v3, Lwu5;->COROUTINE_SUSPENDED:Lwu5;

    if-ne v2, v3, :cond_1

    const-string v0, "BlinkModToken"

    const-string v1, "Bearer read suspended"

    invoke-static {v0, v1}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I

    goto :goto_0

    :cond_1
    const-string v0, "BlinkModToken"

    const-string v1, "Bearer read completed immediately"

    invoke-static {v0, v1}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I

    invoke-static {p0, v2}, Lcom/blinkmap/mod/TokenExportContinuation;->handle(Landroid/app/Activity;Ljava/lang/Object;)V
    :try_end_0
    .catch Ljava/lang/Throwable; {:try_start_0 .. :try_end_0} :catch_0

    :goto_0
    return-void

    :catch_0
    move-exception v0

    const-string v1, "BlinkModToken"

    const-string v2, "Bearer read failed"

    invoke-static {v1, v2, v0}, Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I

    const/4 v0, 0x0

    invoke-static {p0, v0}, Lcom/blinkmap/mod/TokenExportBridge;->deliver(Landroid/app/Activity;Ljava/lang/String;)V

    return-void
.end method

.method public static deliver(Landroid/app/Activity;Ljava/lang/String;)V
    .locals 1

    new-instance v0, Lcom/blinkmap/mod/TokenExportUi;

    invoke-direct {v0, p0, p1}, Lcom/blinkmap/mod/TokenExportUi;-><init>(Landroid/app/Activity;Ljava/lang/String;)V

    invoke-virtual {p0, v0}, Landroid/app/Activity;->runOnUiThread(Ljava/lang/Runnable;)V

    return-void
.end method

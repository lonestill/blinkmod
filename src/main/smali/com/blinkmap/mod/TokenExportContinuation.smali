.class public final Lcom/blinkmap/mod/TokenExportContinuation;
.super Ljo5;
.source "TokenExportContinuation.java"


# instance fields
.field private final activity:Landroid/app/Activity;


# direct methods
.method public constructor <init>(Landroid/app/Activity;)V
    .locals 1

    new-instance v0, Lcom/blinkmap/mod/TokenNoopContinuation;

    invoke-direct {v0}, Lcom/blinkmap/mod/TokenNoopContinuation;-><init>()V

    invoke-direct {p0, v0}, Ljo5;-><init>(Lio5;)V

    iput-object p1, p0, Lcom/blinkmap/mod/TokenExportContinuation;->activity:Landroid/app/Activity;

    return-void
.end method

.method public static handle(Landroid/app/Activity;Ljava/lang/Object;)V
    .locals 2

    invoke-static {p1}, Lrxl;->a(Ljava/lang/Object;)Ljava/lang/Throwable;

    move-result-object v0

    if-nez v0, :cond_0

    instance-of v0, p1, Ljava/lang/String;

    if-eqz v0, :cond_1

    check-cast p1, Ljava/lang/String;

    const-string v0, "BlinkModToken"

    const-string v1, "Bearer resolved"

    invoke-static {v0, v1}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I

    invoke-static {p0, p1}, Lcom/blinkmap/mod/TokenExportBridge;->deliver(Landroid/app/Activity;Ljava/lang/String;)V

    return-void

    :cond_0
    const-string v1, "BlinkModToken"

    const-string v0, "Bearer coroutine failed"

    invoke-static {v1, v0}, Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;)I

    goto :goto_0

    :cond_1
    const-string v1, "BlinkModToken"

    const-string v0, "Bearer result was not a String"

    invoke-static {v1, v0}, Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;)I

    :goto_0
    const/4 p1, 0x0

    invoke-static {p0, p1}, Lcom/blinkmap/mod/TokenExportBridge;->deliver(Landroid/app/Activity;Ljava/lang/String;)V

    return-void
.end method


# virtual methods
.method public getContext()Lkotlin/coroutines/CoroutineContext;
    .locals 1

    sget-object v0, Lkotlin/coroutines/g;->b:Lkotlin/coroutines/g;

    return-object v0
.end method

.method public invokeSuspend(Ljava/lang/Object;)Ljava/lang/Object;
    .locals 1

    iget-object v0, p0, Lcom/blinkmap/mod/TokenExportContinuation;->activity:Landroid/app/Activity;

    invoke-static {v0, p1}, Lcom/blinkmap/mod/TokenExportContinuation;->handle(Landroid/app/Activity;Ljava/lang/Object;)V

    sget-object v0, Lkotlin/Unit;->a:Lkotlin/Unit;

    return-object v0
.end method

.class public final Lcom/blinkmap/mod/TokenSessionCheckContinuation;
.super Ljo5;
.source "TokenSessionCheckContinuation.java"


# instance fields
.field private final activity:Landroid/app/Activity;


# direct methods
.method public constructor <init>(Landroid/app/Activity;)V
    .locals 1

    new-instance v0, Lcom/blinkmap/mod/TokenNoopContinuation;

    invoke-direct {v0}, Lcom/blinkmap/mod/TokenNoopContinuation;-><init>()V

    invoke-direct {p0, v0}, Ljo5;-><init>(Lio5;)V

    iput-object p1, p0, Lcom/blinkmap/mod/TokenSessionCheckContinuation;->activity:Landroid/app/Activity;

    return-void
.end method

.method public static handle(Landroid/app/Activity;Ljava/lang/Object;)V
    .locals 1

    invoke-static {p1}, Lrxl;->a(Ljava/lang/Object;)Ljava/lang/Throwable;

    move-result-object v0

    if-nez v0, :cond_0

    invoke-static {p0, p1}, Lcom/blinkmap/mod/TokenLoginOverlay;->onSessionChecked(Landroid/app/Activity;Ljava/lang/Object;)V

    return-void

    :cond_0
    invoke-static {p0}, Lcom/blinkmap/mod/TokenLoginOverlay;->onSessionCheckFailed(Landroid/app/Activity;)V

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

    iget-object v0, p0, Lcom/blinkmap/mod/TokenSessionCheckContinuation;->activity:Landroid/app/Activity;

    invoke-static {v0, p1}, Lcom/blinkmap/mod/TokenSessionCheckContinuation;->handle(Landroid/app/Activity;Ljava/lang/Object;)V

    sget-object v0, Lkotlin/Unit;->a:Lkotlin/Unit;

    return-object v0
.end method

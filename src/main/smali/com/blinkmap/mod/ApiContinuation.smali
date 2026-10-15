.class public final Lcom/blinkmap/mod/ApiContinuation;
.super Ljava/lang/Object;

# interfaces
.implements Ljt5;

# instance fields
.field private final service:Lfyn;

# direct methods
.method public constructor <init>(Lfyn;)V
    .locals 0
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    iput-object p1, p0, Lcom/blinkmap/mod/ApiContinuation;->service:Lfyn;
    return-void
.end method

# virtual methods
.method public getContext()Lkotlin/coroutines/CoroutineContext;
    .locals 1
    sget-object v0, Lkotlin/coroutines/g;->b:Lkotlin/coroutines/g;
    return-object v0
.end method

.method public resumeWith(Ljava/lang/Object;)V
    .locals 1
    invoke-static {p1}, Lnrl;->a(Ljava/lang/Object;)Ljava/lang/Throwable;
    move-result-object v0
    if-eqz v0, :cond_0
    invoke-static {v0}, Lcom/blinkmap/mod/StepHooks;->onApiError(Ljava/lang/Throwable;)V
    return-void

    :cond_0
    invoke-static {p1}, Lcom/blinkmap/mod/StepHooks;->onApiResult(Ljava/lang/Object;)V
    iget-object v0, p0, Lcom/blinkmap/mod/ApiContinuation;->service:Lfyn;
    invoke-static {v0}, Lcom/blinkmap/mod/WorkBridge;->verifyDay(Lfyn;)V
    return-void
.end method

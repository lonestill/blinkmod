.class public final Lcom/blinkmap/mod/UsernameCheckContinuation;
.super Ljava/lang/Object;

# interfaces
.implements Ljt5;

# direct methods
.method public constructor <init>()V
    .locals 0
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method

.method public static handle(Ljava/lang/Object;)V
    .locals 3
    check-cast p0, Lji4;
    iget-boolean v0, p0, Lji4;->a:Z
    iget-object v1, p0, Lji4;->c:Ljava/lang/String;
    invoke-static {v0, v1}, Lcom/blinkmap/mod/StepHooks;->onUsernameCheckResult(ZLjava/lang/String;)V
    return-void
.end method

# virtual methods
.method public getContext()Lkotlin/coroutines/CoroutineContext;
    .locals 1
    sget-object v0, Lkotlin/coroutines/g;->b:Lkotlin/coroutines/g;
    return-object v0
.end method

.method public resumeWith(Ljava/lang/Object;)V
    .locals 2
    invoke-static {p1}, Lnrl;->a(Ljava/lang/Object;)Ljava/lang/Throwable;
    move-result-object v0
    if-eqz v0, :cond_0
    invoke-static {v0}, Lcom/blinkmap/mod/StepHooks;->onUsernameCheckError(Ljava/lang/Throwable;)V
    return-void

    :cond_0
    :try_start_0
    invoke-static {p1}, Lcom/blinkmap/mod/UsernameCheckContinuation;->handle(Ljava/lang/Object;)V
    :try_end_0
    .catch Ljava/lang/Throwable; {:try_start_0 .. :try_end_0} :catch_0
    return-void

    :catch_0
    move-exception v1
    invoke-static {v1}, Lcom/blinkmap/mod/StepHooks;->onUsernameCheckError(Ljava/lang/Throwable;)V
    return-void
.end method

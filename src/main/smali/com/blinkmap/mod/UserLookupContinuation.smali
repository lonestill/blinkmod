.class public final Lcom/blinkmap/mod/UserLookupContinuation;
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
    .locals 12
    check-cast p0, Lwik;

    iget-object v0, p0, Lwik;->a:Lw0q;
    invoke-interface {v0}, Lw0q;->getValue()J
    move-result-wide v1

    iget-object v0, p0, Lwik;->c:Lqhk;
    iget-object v3, v0, Lqhk;->b:Ljava/lang/String;
    iget-object v4, v0, Lqhk;->c:Ljava/lang/String;
    iget-object v5, v0, Lqhk;->f:Ljava/lang/String;
    invoke-static {v3, v4, v5, v1, v2}, Lcom/blinkmap/mod/StepHooks;->onUserLookupResult(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;J)V

    iget-object v8, p0, Lwik;->d:Lqf;
    iget v0, v8, Lqf;->a:I
    int-to-long v0, v0

    iget-object v8, p0, Lwik;->e:Lph;
    iget-wide v2, v8, Lph;->a:J

    iget-object v8, p0, Lwik;->f:Lvg;
    iget-wide v4, v8, Lvg;->a:J

    iget-object v8, p0, Lwik;->g:Ldf;
    iget-wide v6, v8, Ldf;->a:J

    invoke-static/range {v0 .. v7}, Lcom/blinkmap/mod/StepHooks;->onUserLookupStats(JJJJ)V
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
    invoke-static {v0}, Lcom/blinkmap/mod/StepHooks;->onUserLookupError(Ljava/lang/Throwable;)V
    return-void

    :cond_0
    :try_start_0
    invoke-static {p1}, Lcom/blinkmap/mod/UserLookupContinuation;->handle(Ljava/lang/Object;)V
    :try_end_0
    .catch Ljava/lang/Throwable; {:try_start_0 .. :try_end_0} :catch_0
    return-void

    :catch_0
    move-exception v1
    invoke-static {v1}, Lcom/blinkmap/mod/StepHooks;->onUserLookupError(Ljava/lang/Throwable;)V
    return-void
.end method

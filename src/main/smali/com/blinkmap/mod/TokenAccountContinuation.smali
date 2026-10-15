.class public final Lcom/blinkmap/mod/TokenAccountContinuation;
.super Ljo5;
.source "TokenAccountContinuation.java"


# instance fields
.field private final activity:Landroid/app/Activity;

.field private final store:Lcym;

.field private final token:Ljava/lang/String;


# direct methods
.method public constructor <init>(Landroid/app/Activity;Lcym;Ljava/lang/String;)V
    .locals 1

    new-instance v0, Lcom/blinkmap/mod/TokenNoopContinuation;

    invoke-direct {v0}, Lcom/blinkmap/mod/TokenNoopContinuation;-><init>()V

    invoke-direct {p0, v0}, Ljo5;-><init>(Lio5;)V

    iput-object p1, p0, Lcom/blinkmap/mod/TokenAccountContinuation;->activity:Landroid/app/Activity;

    iput-object p2, p0, Lcom/blinkmap/mod/TokenAccountContinuation;->store:Lcym;

    iput-object p3, p0, Lcom/blinkmap/mod/TokenAccountContinuation;->token:Ljava/lang/String;

    return-void
.end method

.method public static handle(Lcom/blinkmap/mod/TokenAccountContinuation;Ljava/lang/Object;)V
    .locals 4

    :try_start_0
    invoke-static {p1}, Lrxl;->a(Ljava/lang/Object;)Ljava/lang/Throwable;

    move-result-object v0

    if-eqz v0, :cond_0

    iget-object v0, p0, Lcom/blinkmap/mod/TokenAccountContinuation;->activity:Landroid/app/Activity;

    invoke-static {v0}, Lcom/blinkmap/mod/TokenLoginOverlay;->onSaveFailed(Landroid/app/Activity;)V

    return-void

    :cond_0
    new-instance v0, Lcom/blinkmap/mod/TokenBearerContinuation;

    iget-object v1, p0, Lcom/blinkmap/mod/TokenAccountContinuation;->activity:Landroid/app/Activity;

    invoke-direct {v0, v1}, Lcom/blinkmap/mod/TokenBearerContinuation;-><init>(Landroid/app/Activity;)V

    iget-object v1, p0, Lcom/blinkmap/mod/TokenAccountContinuation;->store:Lcym;

    iget-object v2, p0, Lcom/blinkmap/mod/TokenAccountContinuation;->token:Ljava/lang/String;

    invoke-virtual {v1, v2, v0}, Lcym;->n(Ljava/lang/String;Ljo5;)Ljava/lang/Object;

    move-result-object v2

    sget-object v3, Lwu5;->COROUTINE_SUSPENDED:Lwu5;

    if-eq v2, v3, :cond_1

    invoke-static {v0, v2}, Lcom/blinkmap/mod/TokenBearerContinuation;->handle(Lcom/blinkmap/mod/TokenBearerContinuation;Ljava/lang/Object;)V
    :try_end_0
    .catch Ljava/lang/Throwable; {:try_start_0 .. :try_end_0} :catch_0

    :cond_1
    return-void

    :catch_0
    move-exception v0

    iget-object v1, p0, Lcom/blinkmap/mod/TokenAccountContinuation;->activity:Landroid/app/Activity;

    invoke-static {v1}, Lcom/blinkmap/mod/TokenLoginOverlay;->onSaveFailed(Landroid/app/Activity;)V

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

    invoke-static {p0, p1}, Lcom/blinkmap/mod/TokenAccountContinuation;->handle(Lcom/blinkmap/mod/TokenAccountContinuation;Ljava/lang/Object;)V

    sget-object v0, Lkotlin/Unit;->a:Lkotlin/Unit;

    return-object v0
.end method

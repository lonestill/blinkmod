.class public final Lcom/blinkmap/mod/BumpContinuation;
.super Ljava/lang/Object;

# interfaces
.implements Ljt5;

# direct methods
.method public constructor <init>()V
    .locals 0
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
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
    const-string v0, "Ошибка при отправке бампа"
    invoke-static {v0}, Lcom/blinkmap/mod/StepHooks;->toast(Ljava/lang/String;)V
    return-void

    :cond_0
    const-string v0, "Бамп успешно отправлен!"
    invoke-static {v0}, Lcom/blinkmap/mod/StepHooks;->toast(Ljava/lang/String;)V
    return-void
.end method

.class public final Lcom/blinkmap/mod/FriendListContinuation;
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
    check-cast p0, Ljava/util/List;
    new-instance v0, Ljava/lang/StringBuilder;
    invoke-direct {v0}, Ljava/lang/StringBuilder;-><init>()V
    const/4 v1, 0x0

    :goto_0
    invoke-interface {p0}, Ljava/util/List;->size()I
    move-result v2
    if-ge v1, v2, :cond_2
    invoke-interface {p0, v1}, Ljava/util/List;->get(I)Ljava/lang/Object;
    move-result-object v2
    check-cast v2, Lh02;

    iget-object v3, v2, Lh02;->b:Ljava/lang/String;
    if-nez v3, :cond_0
    const-string v3, ""
    :cond_0
    invoke-virtual {v0, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    const-string v4, "\t"
    invoke-virtual {v0, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    iget-object v3, v2, Lh02;->c:Ljava/lang/String;
    if-nez v3, :cond_1
    const-string v3, ""
    :cond_1
    invoke-virtual {v0, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    invoke-virtual {v0, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    iget-object v3, v2, Lh02;->a:Lw0q;
    invoke-interface {v3}, Lw0q;->getValue()J
    move-result-wide v5
    invoke-virtual {v0, v5, v6}, Ljava/lang/StringBuilder;->append(J)Ljava/lang/StringBuilder;
    const-string v3, "\n"
    invoke-virtual {v0, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    add-int/lit8 v1, v1, 0x1
    goto :goto_0

    :cond_2
    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
    move-result-object v0
    invoke-static {v0, v1}, Lcom/blinkmap/mod/StepHooks;->onFriendsResult(Ljava/lang/String;I)V
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
    invoke-static {v0}, Lcom/blinkmap/mod/StepHooks;->onFriendsError(Ljava/lang/Throwable;)V
    return-void

    :cond_0
    :try_start_0
    invoke-static {p1}, Lcom/blinkmap/mod/FriendListContinuation;->handle(Ljava/lang/Object;)V
    :try_end_0
    .catch Ljava/lang/Throwable; {:try_start_0 .. :try_end_0} :catch_0
    return-void

    :catch_0
    move-exception v1
    invoke-static {v1}, Lcom/blinkmap/mod/StepHooks;->onFriendsError(Ljava/lang/Throwable;)V
    return-void
.end method

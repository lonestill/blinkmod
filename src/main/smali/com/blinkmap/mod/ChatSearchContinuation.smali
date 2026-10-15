.class public final Lcom/blinkmap/mod/ChatSearchContinuation;
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
    .locals 10
    check-cast p0, Lai4;
    iget-object v0, p0, Lai4;->a:Ljava/util/List;
    new-instance v1, Ljava/lang/StringBuilder;
    invoke-direct {v1}, Ljava/lang/StringBuilder;-><init>()V
    const/4 v2, 0x0

    :goto_0
    invoke-interface {v0}, Ljava/util/List;->size()I
    move-result v3
    if-ge v2, v3, :cond_1
    invoke-interface {v0, v2}, Ljava/util/List;->get(I)Ljava/lang/Object;
    move-result-object v4
    check-cast v4, Lah4;
    iget-object v5, v4, Lah4;->c:Ljava/lang/String;
    if-nez v5, :cond_0
    const-string v5, ""
    :cond_0
    invoke-virtual {v1, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    const-string v6, "\t"
    invoke-virtual {v1, v6}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    iget-wide v7, v4, Lah4;->a:J
    invoke-virtual {v1, v7, v8}, Ljava/lang/StringBuilder;->append(J)Ljava/lang/StringBuilder;
    invoke-virtual {v1, v6}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    iget v7, v4, Lah4;->f:I
    invoke-virtual {v1, v7}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;
    const-string v5, "\n"
    invoke-virtual {v1, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    add-int/lit8 v2, v2, 0x1
    goto :goto_0

    :cond_1
    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
    move-result-object v1
    invoke-static {v1, v2}, Lcom/blinkmap/mod/StepHooks;->onChatSearchResult(Ljava/lang/String;I)V
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
    invoke-static {v0}, Lcom/blinkmap/mod/StepHooks;->onChatSearchError(Ljava/lang/Throwable;)V
    return-void

    :cond_0
    :try_start_0
    invoke-static {p1}, Lcom/blinkmap/mod/ChatSearchContinuation;->handle(Ljava/lang/Object;)V
    :try_end_0
    .catch Ljava/lang/Throwable; {:try_start_0 .. :try_end_0} :catch_0
    return-void

    :catch_0
    move-exception v1
    invoke-static {v1}, Lcom/blinkmap/mod/StepHooks;->onChatSearchError(Ljava/lang/Throwable;)V
    return-void
.end method

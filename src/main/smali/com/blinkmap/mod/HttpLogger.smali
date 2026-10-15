.class public final Lcom/blinkmap/mod/HttpLogger;
.super Ljava/lang/Object;

# direct methods
.method private constructor <init>()V
    .locals 0
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method

.method public static getTag(Ljava/lang/String;)Ljava/lang/String;
    .locals 2
    if-nez p0, :cond_0
    const-string p0, "BLINK_HTTP"
    return-object p0

    :cond_0
    const-string v0, "blink-maps"
    invoke-virtual {p0, v0}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z
    move-result v0
    if-nez v0, :cond_1
    const-string v0, "/api/"
    invoke-virtual {p0, v0}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z
    move-result v0
    if-eqz v0, :cond_2

    :cond_1
    const-string p0, "BLINK_HTTP"
    return-object p0

    :cond_2
    const-string v0, "yandex"
    invoke-virtual {p0, v0}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z
    move-result v0
    if-nez v0, :cond_3
    const-string v0, "appsflyer"
    invoke-virtual {p0, v0}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z
    move-result v0
    if-nez v0, :cond_3
    const-string v0, "applovin"
    invoke-virtual {p0, v0}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z
    move-result v0
    if-nez v0, :cond_3
    const-string v0, "unity"
    invoke-virtual {p0, v0}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z
    move-result v0
    if-eqz v0, :cond_4

    :cond_3
    const-string p0, "BLINK_HTTP_ADS"
    return-object p0

    :cond_4
    const-string p0, "BLINK_HTTP"
    return-object p0
.end method

.method public static logError(Ljava/lang/Object;Ljava/lang/Throwable;)V
    .locals 3
    :try_start_0
    if-eqz p0, :cond_0
    check-cast p0, Lenl;
    iget-object p0, p0, Lenl;->a:Lutb;
    if-eqz p0, :cond_0
    iget-object p0, p0, Lutb;->i:Ljava/lang/String;
    goto :goto_0

    :cond_0
    const-string p0, "null"

    :goto_0
    invoke-static {p0}, Lcom/blinkmap/mod/HttpLogger;->getTag(Ljava/lang/String;)Ljava/lang/String;
    move-result-object v0
    new-instance v1, Ljava/lang/StringBuilder;
    invoke-direct {v1}, Ljava/lang/StringBuilder;-><init>()V
    const-string v2, "<-- FAILED "
    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    invoke-virtual {v1, p0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    const-string p0, " : "
    invoke-virtual {v1, p0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    if-eqz p1, :cond_1
    invoke-virtual {p1}, Ljava/lang/Throwable;->toString()Ljava/lang/String;
    move-result-object p0
    invoke-virtual {v1, p0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    goto :goto_1

    :cond_1
    const-string p0, "unknown error"
    invoke-virtual {v1, p0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    :goto_1
    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
    move-result-object p0
    invoke-static {v0, p0}, Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;)I
    const-string p1, "BLINK_HTTP"
    invoke-virtual {p1, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
    move-result p1
    if-eqz p1, :cond_2
    invoke-static {p0}, Lcom/blinkmap/mod/StepHooks;->log(Ljava/lang/String;)V
    :try_end_0
    .catch Ljava/lang/Throwable; {:try_start_0 .. :try_end_0} :catch_0

    :catch_0
    :cond_2
    return-void
.end method

.method public static logLong(Ljava/lang/String;Ljava/lang/String;)V
    .locals 3
    if-nez p1, :cond_0
    return-void

    :cond_0
    :goto_0
    invoke-virtual {p1}, Ljava/lang/String;->length()I
    move-result v0
    const/16 v1, 0xbb8
    if-le v0, v1, :cond_1
    const/4 v0, 0x0
    invoke-virtual {p1, v0, v1}, Ljava/lang/String;->substring(II)Ljava/lang/String;
    move-result-object v0
    invoke-static {p0, v0}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I
    invoke-virtual {p1, v1}, Ljava/lang/String;->substring(I)Ljava/lang/String;
    move-result-object p1
    goto :goto_0

    :cond_1
    invoke-static {p0, p1}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I
    return-void
.end method

.method public static logRequest(Ljava/lang/Object;)V
    .locals 5
    if-nez p0, :cond_0
    return-void

    :cond_0
    :try_start_0
    check-cast p0, Lenl;
    iget-object v0, p0, Lenl;->a:Lutb;
    if-eqz v0, :cond_1
    iget-object v0, v0, Lutb;->i:Ljava/lang/String;
    goto :goto_0

    :cond_1
    const-string v0, "null"

    :goto_0
    iget-object v1, p0, Lenl;->b:Ljava/lang/String;
    invoke-static {v0}, Lcom/blinkmap/mod/HttpLogger;->getTag(Ljava/lang/String;)Ljava/lang/String;
    move-result-object v2
    new-instance v3, Ljava/lang/StringBuilder;
    invoke-direct {v3}, Ljava/lang/StringBuilder;-><init>()V
    const-string v4, "--> "
    invoke-virtual {v3, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    invoke-virtual {v3, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    const-string v1, " "
    invoke-virtual {v3, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    invoke-virtual {v3, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    invoke-virtual {v3}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
    move-result-object v0
    invoke-static {v2, v0}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I

    const-string v1, "BLINK_HTTP"
    invoke-virtual {v1, v2}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
    move-result v1
    if-eqz v1, :cond_2
    invoke-static {v0}, Lcom/blinkmap/mod/StepHooks;->log(Ljava/lang/String;)V
    :try_end_0
    .catch Ljava/lang/Throwable; {:try_start_0 .. :try_end_0} :catch_0

    :catch_0
    :cond_2
    return-void
.end method

.method public static interceptRequest(Ljava/lang/Object;)Ljava/lang/Object;
    .locals 3
    if-nez p0, :ret_orig
    :try_start_0
    invoke-static {p0}, Lcom/blinkmap/mod/HttpLogger;->logRequest(Ljava/lang/Object;)V

    invoke-static {}, Lcom/blinkmap/mod/StepHooks;->isGhostVoiceActive()Z
    move-result v0
    if-eqz v0, :ret_orig

    move-object v0, p0
    check-cast v0, Lenl;
    iget-object v1, v0, Lenl;->a:Lutb;
    if-eqz v1, :ret_orig
    iget-object v1, v1, Lutb;->i:Ljava/lang/String;
    if-eqz v1, :ret_orig

    const-string v2, "/api/messenger/audio-stickers/read"
    invoke-virtual {v1, v2}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z
    move-result v2
    if-eqz v2, :ret_orig
    const-string v1, "BLINK_HTTP"
    const-string v2, "[GHOST] Suppressed /api/messenger/audio-stickers/read"
    invoke-static {v1, v2}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I
    :try_end_0
    .catch Ljava/lang/Throwable; {:try_start_0 .. :try_end_0} :catch_0

    :catch_0
    :ret_orig
    return-object p0
.end method

.method public static logResponse(Ljava/lang/Object;)V
    .locals 8
    if-nez p0, :cond_0
    return-void

    :cond_0
    :try_start_0
    check-cast p0, Lnql;
    iget-object v0, p0, Lnql;->b:Lenl;
    if-eqz v0, :cond_1
    iget-object v0, v0, Lenl;->a:Lutb;
    if-eqz v0, :cond_1
    iget-object v0, v0, Lutb;->i:Ljava/lang/String;
    goto :goto_0

    :cond_1
    const-string v0, "null"

    :goto_0
    invoke-static {v0}, Lcom/blinkmap/mod/HttpLogger;->getTag(Ljava/lang/String;)Ljava/lang/String;
    move-result-object v1
    iget v2, p0, Lnql;->e:I
    iget-object v3, p0, Lnql;->d:Ljava/lang/String;
    new-instance v4, Ljava/lang/StringBuilder;
    invoke-direct {v4}, Ljava/lang/StringBuilder;-><init>()V
    const-string v5, "<-- "
    invoke-virtual {v4, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    invoke-virtual {v4, v2}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;
    const-string v2, " "
    invoke-virtual {v4, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    invoke-virtual {v4, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    invoke-virtual {v4, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    invoke-virtual {v4, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    invoke-virtual {v4}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
    move-result-object v2
    invoke-static {v1, v2}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I

    const-string v3, "BLINK_HTTP"
    invoke-virtual {v3, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
    move-result v3
    if-eqz v3, :cond_2
    invoke-static {v2}, Lcom/blinkmap/mod/StepHooks;->log(Ljava/lang/String;)V

    :cond_2
    iget-object v2, p0, Lnql;->h:Lzql;
    if-eqz v2, :cond_3
    invoke-virtual {v2}, Lzql;->source()Lgj2;
    move-result-object v2
    if-eqz v2, :cond_3
    const-wide/32 v3, 0x1000000
    invoke-interface {v2, v3, v4}, Lgj2;->request(J)Z
    invoke-interface {v2}, Lgj2;->getBuffer()Lii2;
    move-result-object v2
    if-eqz v2, :cond_3
    invoke-virtual {v2}, Lii2;->clone()Ljava/lang/Object;
    move-result-object v2
    check-cast v2, Lii2;
    invoke-virtual {v2}, Lii2;->readUtf8()Ljava/lang/String;
    move-result-object v2
    if-eqz v2, :cond_3
    const-string v3, "<-- BODY: "
    invoke-static {v1, v3, v2}, Lcom/blinkmap/mod/HttpLogger;->logWithPrefix(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V
    :try_end_0
    .catch Ljava/lang/Throwable; {:try_start_0 .. :try_end_0} :catch_0

    :catch_0
    :cond_3
    return-void
.end method

.method public static interceptResponse(Ljava/lang/Object;)Ljava/lang/Object;
    .locals 4
    if-nez p0, :ret_orig
    :try_start_0
    invoke-static {p0}, Lcom/blinkmap/mod/HttpLogger;->logResponse(Ljava/lang/Object;)V

    invoke-static {}, Lcom/blinkmap/mod/StepHooks;->isAntiSpoofShieldActive()Z
    move-result v0
    if-eqz v0, :ret_orig

    move-object v0, p0
    check-cast v0, Lnql;
    iget-object v1, v0, Lnql;->b:Lenl;
    if-eqz v1, :ret_orig
    iget-object v1, v1, Lenl;->a:Lutb;
    if-eqz v1, :ret_orig
    iget-object v1, v1, Lutb;->i:Ljava/lang/String;
    if-eqz v1, :ret_orig
    const-string v2, "/spoofing/status"
    invoke-virtual {v1, v2}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z
    move-result v1
    if-eqz v1, :ret_orig

    # Anti-spoof shield triggered: spoof JSON to {"status":false}
    invoke-virtual {v0}, Lnql;->n()Llql;
    move-result-object v1
    const-string v2, "{\"status\":false}"
    const/4 v3, 0x0
    invoke-static {v2, v3}, Lyql;->b(Ljava/lang/String;Lrof;)Lxql;
    move-result-object v2
    iput-object v2, v1, Llql;->g:Lzql;
    invoke-virtual {v1}, Llql;->a()Lnql;
    move-result-object p0
    const-string v0, "BLINK_HTTP"
    const-string v1, "[SHIELD] Forged spoofing/status -> status:false"
    invoke-static {v0, v1}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I
    :try_end_0
    .catch Ljava/lang/Throwable; {:try_start_0 .. :try_end_0} :catch_0

    :catch_0
    :ret_orig
    return-object p0
.end method

.method public static logWithPrefix(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V
    .locals 1
    if-nez p2, :cond_0
    return-void

    :cond_0
    new-instance v0, Ljava/lang/StringBuilder;
    invoke-direct {v0}, Ljava/lang/StringBuilder;-><init>()V
    if-eqz p1, :cond_1
    invoke-virtual {v0, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    :cond_1
    invoke-virtual {v0, p2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
    move-result-object v0
    invoke-static {p0, v0}, Lcom/blinkmap/mod/HttpLogger;->logLong(Ljava/lang/String;Ljava/lang/String;)V
    return-void
.end method

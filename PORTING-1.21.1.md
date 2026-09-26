# Port para Minecraft 1.21.1

Validação realizada em 26/09/2026, na branch existente `1.21.1`.

## Common / Minecraft 1.21.1

- A consulta de `RecipeType.SMELTING` usa `SingleRecipeInput` com uma cópia do item da mão. Mantém a mesma decisão de receita válida.
- `ItemStack.isSameItemSameComponents` substitui a comparação antiga de tags nas transferências. A implementação vanilla compara o item e seus componentes, independentemente da quantidade.
- A distribuição, prioridade de combustível, divisão das quantidades, coleta e tratamento de inventário cheio foram preservados.

## Networking

- `DistributePacket` e `CollectPacket` implementam `CustomPacketPayload`, com `Type` próprio e `StreamCodec` composto por duas posições.
- Os identificadores continuam `furnacedistributor:distribute` e `furnacedistributor:collect`, criados por `ResourceLocation.fromNamespaceAndPath`.
- O registro usa a sobrecarga não deprecated de `NetworkManager.registerReceiver(Side, Type, StreamCodec, NetworkReceiver)`. O envio usa `sendToServer(payload)`.
- Os handlers continuam enfileirados por `PacketContext.queue`, exigem `ServerPlayer` e executam os gerenciadores no servidor. Nenhuma transferência foi movida para o cliente.

## Fabric

- Metadados atualizados para Minecraft 1.21.1, Java 21 e Architectury 13.
- Entry points, teclas e evento `WorldRenderEvents.AFTER_TRANSLUCENT` preservados.

## NeoForge

- Classes renomeadas para `FurnaceDistributorNeoForge` e `NeoForgeRenderEvents`.
- `FurnaceDistributorNeoForgeClient` usa `@Mod(value = ..., dist = Dist.CLIENT)`, eliminando `DistExecutor`.
- O subscriber client-only mantém `AFTER_TRANSLUCENT_BLOCKS`, `getPoseStack()` e `getCamera()`.
- No FML 4.0.44 fornecido pelo NeoForge 21.1.251, `EventBusSubscriber.bus` está deprecated e é ignorado. A seleção automática encaminha `RenderLevelStageEvent` ao game event bus; isso foi confirmado no log de startup.

## Gradle / metadata

- Mantidas as versões e a configuração Architectury/Fabric/NeoForge existentes, incluindo Java 21.
- `mods.toml` migrado para `META-INF/neoforge.mods.toml`, com dependências `type = "required"` e expansão das propriedades Gradle.
- Constraints NeoForge: Minecraft `[1.21.1]`, NeoForge `[21.1.251,21.2)`, Architectury `[13.0.8,14)`, Java `[21,)` e javafml `[4,)`.
- `pack.mcmeta` usa resource pack format 34, confirmado no `version.json` de Minecraft 1.21.1.
- Os JARs finais foram inspecionados: contêm os payloads, metadados expandidos e configuração Mixin; o NeoForge não contém o antigo `META-INF/mods.toml`.

## Mixins

- Configuração comum em `JAVA_21`, declarada no Fabric e em `[[mixins]]` no TOML do NeoForge.
- O log NeoForge confirma descoberta, seleção e preparação de `furnacedistributor.mixins.json`.
- As listas de mixins do projeto já estavam vazias e foram preservadas; não há injeções próprias para testar.

## Validação

| Comando | Resultado |
| --- | --- |
| `.\gradlew :common:compileJava` | Primeiro bloqueado pelo cache fora do sandbox; executado com permissão, reproduziu 7 erros e 2 warnings antigos. Após as correções: BUILD SUCCESSFUL. |
| `.\gradlew :fabric:build` | BUILD SUCCESSFUL. |
| `.\gradlew :neoforge:build` | BUILD SUCCESSFUL. |
| `.\gradlew build --warning-mode all` | BUILD SUCCESSFUL. |
| `.\gradlew build` | BUILD SUCCESSFUL na verificação final. |
| `.\gradlew :fabric:runClient` | Startup concluído, mod listado, receivers C2S registrados; encerramento normal, BUILD SUCCESSFUL. |
| `.\gradlew :neoforge:runClient` | Startup concluído, mod listado, entrypoints carregados, Mixin preparado e evento de render registrado; encerramento normal, BUILD SUCCESSFUL. |
| `.\gradlew :fabric:runServer --args nogui --warning-mode all` | Mod e receivers carregados em ambiente SERVER; parou no EULA não aceito. JVM encerrada explicitamente após a verificação; tarefa terminou com código -1 por esse encerramento. |
| `.\gradlew :neoforge:runServer --args nogui --warning-mode all` | Servidor dedicado chegou a `Done`. Somente o entrypoint comum foi carregado. JVM encerrada após a verificação; tarefa terminou com código -1 por esse encerramento. |
| `.\gradlew :common:transformProductionNeoForge --rerun --warning-mode all` | BUILD SUCCESSFUL; transformação reexecutada para investigar depreciações. |

Os servidores foram encerrados pelo processo porque o stdin das sessões Gradle estava fechado e não permitia enviar `stop`. Nenhum EULA foi editado. As pastas de execução são ignoradas pelo Git. Não houve alteração da branch nem uso de `--refresh-dependencies`.

O aviso de depreciação identificado em `prepareArchitecturyTransformer` é do plugin Architectury: acesso a `Task.project` durante a execução, incompatível futuramente com Gradle 10. Não foi suprimido nem motivou downgrade. O Loom também informa que o projeto está no OneDrive.

Não existem testes automatizados no projeto (`test NO-SOURCE`). Os testes de startup não comprovam a distribuição/coleta em um mundo ou o resultado visual dos overlays. A inspeção visual foi bloqueada pela aprovação automática do computer-use para Java. Não há erro de compilação restante.

Logs desta validação foram preservados localmente em `build/port-validation/` (diretório ignorado pelo Git).

## APIs conferidas

As assinaturas foram verificadas nos sources remapeados locais de Minecraft 1.21.1, Architectury 13.0.8, NeoForge 21.1.251 e FML 4.0.44. O entrypoint client-only e o formato dos metadados também foram conferidos na [documentação oficial NeoForge 1.21.1](https://docs.neoforged.net/docs/1.21.1/gettingstarted/modfiles/).

# Autorização de distribuição e coleta

## Estado em 10/10/2026

O bypass de proteções implementadas somente por eventos de interação está
confirmado. **Esta etapa não corrige esse bypass em produção.** Acrescenta
regressões da autorização integral existente e registra por que não foi
adotado um clique sintético como consulta de permissão.

Versões verificadas: Minecraft 1.21.1, Fabric Loader 0.19.5,
Fabric API 0.116.17+1.21.1 (events-interaction-v0 0.7.14+ba9dae0619),
NeoForge 21.1.251, Architectury API 13.0.8 e Iron Furnaces NeoForge 4.3.2.

## Fluxo atual

Os receivers de `DistributePacket` e `CollectPacket` enfileiram a operação
na thread do servidor. Os dois managers chamam `OperationGuard.find()`
antes de planejar ou modificar inventários.

O guard verifica estado do jogador, menu, cooldown, volume, distância,
altura, borda do mundo, chunks carregados e quantidade de fornalhas.
Depois consulta `Level.mayInteract()` e `FurnaceAccess.canAccess()` para
todos os destinos. Uma recusa retorna `null`; nenhum manager transfere
itens ou libera XP nessa situação, mesmo com `allowPartialDistribution=true`.

Em `ServerLevel`, `mayInteract()` cobre proteção de spawn e borda do mundo.
Vanilla usa `canOpen()` para o lock da fornalha. O adapter Iron verifica
identidade, mundo e remoção da entidade; o Iron não fornece um lock equivalente.
Essas verificações não equivalem a uma autorização de claims de outros mods.

## Confirmação nas plataformas

No Fabric, o callback `UseBlockCallback.EVENT` é invocado pelo mixin do
gerenciador de interação do servidor, no caminho de uso de bloco.
O `OperationGuard` e as transferências diretas não percorrem esse caminho.

No NeoForge 21.1.251, o patch de `ServerPlayerGameMode.useItemOn()` consulta
`CommonHooks.onRightClickBlock()`. O guard e as transferências também não
percorrem esse caminho.

Foi executado um GameTest diagnóstico temporário no NeoForge sem Iron:

1. Criar duas fornalhas acessíveis e um jogador com 16 cobblestones.
2. Registrar um listener de `RightClickBlock`, restrito ao jogador do teste,
   que cancela o evento na segunda fornalha.
3. Invocar o hook nativo e confirmar que a segunda fornalha é recusada.
4. Distribuir na área com os dois destinos.
5. Preparar outputs de 10 e 5 iron ingots e coletar a mesma área.
6. Exigir zero itens distribuídos e coletados.
7. Remover o listener em `finally`.

Resultado: o diagnóstico falhou, como esperado, com
`native event denied second furnace, but distribution moved 16 and collection moved 15`.
Os outros sete testes passaram. O log foi preservado localmente em
`docs/validation/permissions-reproduction.log`, ignorado pelo Git.
O diagnóstico temporário foi removido da suíte regular; sua remoção não
significa que o problema foi corrigido.

No Fabric, o bypass foi confirmado pela leitura do callback e do mixin da
versão exata utilizada. Não foi executado um servidor Fabric com uma proteção
instalada; não se declara essa verificação como teste de integração Fabric.

Fontes oficiais correspondentes, além dos sources/patches locais das versões exatas:

- [Fabric UseBlockCallback](https://github.com/FabricMC/fabric/blob/1.21.1/fabric-events-interaction-v0/src/main/java/net/fabricmc/fabric/api/event/player/UseBlockCallback.java)
- [Fabric ServerPlayerInteractionManagerMixin](https://github.com/FabricMC/fabric/blob/1.21.1/fabric-events-interaction-v0/src/main/java/net/fabricmc/fabric/mixin/event/interaction/ServerPlayerInteractionManagerMixin.java)
- [NeoForge ServerPlayerGameMode patch](https://github.com/neoforged/NeoForge/blob/1.21.1/patches/net/minecraft/server/level/ServerPlayerGameMode.java.patch)
- [NeoForge CommonHooks](https://github.com/neoforged/NeoForge/blob/1.21.1/src/main/java/net/neoforged/neoforge/common/CommonHooks.java)

## Por que não disparar cliques sintéticos

Os eventos recebem o jogador e o mundo reais. Não há um modo de consulta
sem efeitos colaterais ou uma lista que separe listeners de proteção de
listeners que executam gameplay. Um listener pode alterar um inventário,
abrir um menu ou alterar o mundo enquanto processa o primeiro destino,
antes da recusa do segundo.

No Fabric, um resultado diferente de `PASS` pode representar uma ação já
executada, não somente uma recusa. No NeoForge, cancelamento e `useBlock=FALSE`
podem negar a interação, mas publicar o evento ainda executa seus listeners.
Invocar apenas o evento Architectury também não consulta todos os listeners
registrados diretamente nas APIs nativas.

Revalidar estado depois desses eventos não desfaz seus efeitos. Restaurar
pilhas ou o mundo como rollback poderia apagar mudanças legítimas de outros
mods. Por isso essa alternativa não garante a autorização integral sem
transferências parciais e preservação do gameplay pedidas nesta etapa.

## Correção necessária

Identificar os mods de proteção e suas versões e usar sua API pública de
consulta de acesso, sem executar uma interação. A consulta deve receber o
jogador real, a dimensão, a posição e a permissão de acesso ao inventário.

Inserir essa consulta no preflight de `OperationGuard`, mantendo todos os
controles existentes, e recusar a operação inteira se qualquer destino for
negado. A integração deve ser opcional e isolada na plataforma, como ocorre
com Iron Furnaces. Nenhuma API ou dependência de claims foi presumida nesta etapa.

## Regressões permanentes

- `VanillaGameTests.secondLockedFurnaceRejectsDistributionAndCollection`:
  segunda fornalha com lock; primeira fornalha e mão intactas na distribuição,
  outputs e inventário intactos na coleta e nenhuma liberação de XP.
  Verifica que a opção de distribuição parcial não relaxa a autorização.
- `IronGameTests.lockedSecondFurnacePreservesFactoryInputsOutputsAndXp`:
  primeira fornalha Iron Factory e segunda vanilla com lock; nenhum input ou
  output muda e o ledger de XP da Factory permanece intacto.

Esses testes verificam os controles já existentes. Não comprovam correção
do bypass por eventos nem compatibilidade com um provedor de claims específico.

Comandos de validação:

```powershell
.\gradlew.bat :common:build :fabric:build :neoforge:build --offline --console=plain
.\gradlew.bat :neoforge:runIntegrationTest -PintegrationTests=true -PwithIronFurnaces=false --offline --console=plain
.\gradlew.bat :neoforge:runIntegrationTest -PintegrationTests=true -PwithIronFurnaces=true --offline --console=plain
python -B tools/validate_artifacts.py
```

Resultados finais:

- Builds de common, Fabric e NeoForge: `BUILD SUCCESSFUL`.
- Regressões common, executadas pelo build: 1.216.563 verificações passaram.
- NeoForge sem Iron Furnaces: 7/7 GameTests passaram.
- NeoForge com Iron Furnaces 4.3.2: 30/30 GameTests passaram.
- Validação dos dois JARs: classes compartilhadas, Java 21, idiomas, mixins
  e metadados dos loaders aprovados.
- `git diff --check`: aprovado. Nenhum commit foi criado.

O único teste que falhou foi o diagnóstico temporário do bypass, descrito
acima. As suítes aprovadas não demonstram que esse bypass foi eliminado.

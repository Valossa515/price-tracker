# Diretrizes para Claude neste projeto

## Antes de explorar código
- Quando precisar entender uma lib que tem CLAUDE.md (ex: spring-courier), LEIA O CLAUDE.md DELA primeiro. Não Read README/source até ter pergunta específica.
- Para checar API de uma classe: `grep -n "public.*method"` em vez de Read do arquivo inteiro.

## Saídas de comandos
- `mvn`: SEMPRE `-q` + `2>&1 | tail -30`. Stacktraces filtrar com `grep -E 'ERROR|Caused by' | head -20`.
- `aws`: SEMPRE `--query 'Path.To.Field' --output text` para extrair só o que preciso. JSON completo só para inspeção inicial e ainda assim com `--query` que limita.
- `curl` em smoke tests: `-s -o /dev/null -w "HTTP %{http_code}\n"` por padrão. Body só quando preciso confirmar conteúdo, e com `head -c 100`.
- Polls: sem print de progresso por iteração. Só relatar resultado final.

## Antes de Edit/Write em código que usa lib externa
- Confirmar nome de pacote, método e signature via `grep` na lib ANTES de escrever. Não confiar em memória de README.

## Respostas
- Bullets curtos. Sem tabelas decorativas (deixar pra quando há ≥4 linhas de dados reais).
- Não re-explicar a arquitetura inteira a cada turn. Citar só o que mudou.
- Confirmações de "tudo OK" em 1-2 frases, não em seção formatada.

## Iteração de erros
- Se uma chamada falhar por algo que dava para prevenir lendo doc/code da lib, na próxima vez leia ANTES.

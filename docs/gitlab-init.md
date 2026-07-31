# GitLab local init

Steps to configure a freshly started local GitLab CE (`docker compose --profile gitlab up`) so it's ready for testing this integration.

## 1. Wait for GitLab to be healthy

```
docker compose logs -f gitlab
```

Wait until it reports being up (first boot takes several minutes), then open `http://localhost:8080`.

## 2. Log in as root

`compose.yaml` sets `GITLAB_ROOT_PASSWORD`, so on a fresh volume the root password is `localtest12345!` (log in as `root` at `http://localhost:8080` if you want to use the UI too).

This only applies to a brand-new database — it has no effect on a volume that already has a root user seeded. If you're carrying over an older volume, either fetch the previously auto-generated password instead (`docker compose exec gitlab cat /etc/gitlab/initial_root_password`, valid for 24h after first startup) or wipe the volumes (`docker compose down -v`) to get a fresh install with the password above.

## 3. Mint a Personal Access Token for root

No token exists yet, so create one directly via the Rails console rather than curling a session:

> **This takes a few minutes and prints nothing while it runs.** `gitlab-rails runner` reboots the whole Rails app (autoloading + all initializers, including a DB partition-sync check) from scratch on every invocation, even if GitLab has been up and serving pages for a while. 3-5 minutes with zero output is normal — let it finish rather than cancelling; interrupting it mid-boot throws a Ruby stack trace but doesn't leave anything broken, you can just rerun it.

```
docker compose exec -T gitlab gitlab-rails runner "
  token = User.find_by_username('root').personal_access_tokens.create(
    scopes: [:api], name: 'local-test', expires_at: 30.days.from_now)
  token.set_token('local-test-token')
  token.save!
  puts token.token
"
```

Export the printed value:

```
export GITLAB_TOKEN=<printed token>
```

## 4. Create the parent hackathon group

This matches `gitlab.parent-group-path`.

```
curl --header "PRIVATE-TOKEN: $GITLAB_TOKEN" \
  --data "name=hackathons&path=hackathons&visibility=private" \
  http://localhost:8080/api/v4/groups
```

## 5. Create test users

Usernames match the `gitlab_user` values used in the [README's event examples](../README.md#event-examples) (`user1`, `user2`).

```
curl --header "PRIVATE-TOKEN: $GITLAB_TOKEN" \
  --data "email=user1@example.com&username=user1&name=User1+Test&password=Hackathon2026&skip_confirmation=true" \
  http://localhost:8080/api/v4/users

curl --header "PRIVATE-TOKEN: $GITLAB_TOKEN" \
  --data "email=user2@example.com&username=user2&name=User2+Test&password=Hackathon2026&skip_confirmation=true" \
  http://localhost:8080/api/v4/users
```

## 6. Point the app at this instance

Set for `bootRun`:

```
gitlab.url=http://localhost:8080
gitlab.access-token=<value of $GITLAB_TOKEN>
gitlab.parent-group-path=hackathons
```

As a full `bootRun` invocation:

```
./gradlew bootRun --args="--gitlab.url=http://localhost:8080 --gitlab.access-token=$GITLAB_TOKEN --gitlab.parent-group-path=hackathons"
```

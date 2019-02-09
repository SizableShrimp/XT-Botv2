package me.sizableshrimp.discordbot.music.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import me.sizableshrimp.discordbot.music.Music;
import me.sizableshrimp.discordbot.music.MusicPermission;
import reactor.core.publisher.Mono;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DisconnectCommand extends AbstractMusicCommand {
    @Override
    public CommandInfo getInfo() {
        return new CommandInfo("%cmdname%",
                "Disconnects from the voice channel and stops playing music.");
    }

    @Override
    public Set<MusicPermission> getRequiredPermissions() {
        return EnumSet.of(MusicPermission.DJ, MusicPermission.ALONE);
    }

    @Override
    public Set<String> getNames() {
        return Stream.of("disconnect", "leave").collect(Collectors.toSet());
    }

    @Override
    protected Mono<Message> run(MessageCreateEvent event, String[] args) {
        if (!event.getMember().isPresent()) return Mono.empty();
        return event.getMessage().getChannel()
                .filterWhen(c -> hasPermission(event))
                .flatMap(c -> Music.getBotConnectedVoiceChannel(event.getClient(), event.getGuildId().get())
                        .flatMap(Mono::justOrEmpty)
                        .flatMap(voiceChannel -> {
                            Music.disconnectBotFromChannel(event.getGuildId().get());
                            return sendMessage("Left `" + voiceChannel.getName() + "`", c);
                        })
                        .switchIfEmpty(sendMessage("I am not connected to a voice channel.", c)));
    }
}

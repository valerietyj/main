package owlmoney.logic.command.goals;

import owlmoney.logic.command.Command;
import owlmoney.model.goals.exception.GoalsException;
import owlmoney.model.profile.Profile;
import owlmoney.ui.Ui;

import java.util.logging.Logger;

import static owlmoney.commons.log.LogsCenter.getLogger;

/**
 * Executes DeleteGoalsCommand to delete goal object.
 */
public class DeleteGoalsCommand extends Command {

    private final String name;
    private static final Logger logger = getLogger(DeleteGoalsCommand.class);

    /**
     * Creates an instance of DeleteGoalsCommand.
     *
     * @param name Name of goal object to delete
     */
    public DeleteGoalsCommand(String name) {
        this.name = name;
    }

    /**
     * Executes the function to delete goals in the profile.
     *
     * @param profile Profile of the user.
     * @param ui      Ui of OwlMoney.
     * @return false so OwlMoney will not terminate yet.
     * @throws GoalsException If delete invalid goals.
     */
    @Override
    public boolean execute(Profile profile, Ui ui) throws GoalsException {
        profile.profileDeleteGoals(name, ui);
        logger.info("Successful execution of deleting goals");
        return this.isExit;
    }
}
